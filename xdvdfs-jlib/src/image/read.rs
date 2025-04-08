use super::img::open_image_raw;
use anyhow::{anyhow, bail};
use std::fs::File;
use std::io::{ErrorKind, Read, Seek, SeekFrom, Write};
use std::path::{Path, PathBuf, MAIN_SEPARATOR_STR};
use std::sync::atomic::Ordering::Relaxed;
use std::sync::mpsc::Sender;
use xdvdfs::layout::SECTOR_SIZE;
use crate::java::INTERRUPTED;

pub fn stat(img_path: &Path) -> anyhow::Result<[i64; 4]> {
    let mut img = open_image_raw(img_path)?;
    let volume = xdvdfs::read::read_volume(&mut img)?;

    let tree = volume.root_table.file_tree(&mut img)?;

    // We use i64 as `long` is signed in Java, and we don't really need the entire u64 range, do we?
    let mut total_size: i64 = 0;
    let mut file_count: i64 = 0;
    let mut max_sector: i64 = 0;

    for (_dir, file) in &tree {
        // Count files and sum sizes
        if !file.node.dirent.is_directory() {
            total_size = total_size.wrapping_add(file.node.dirent.data.size() as i64);
            file_count += 1;
        }

        // Calculate max sector for trim
        let disk_region = file.node.dirent.data;
        let this_sector = (disk_region.sector as i64) * SECTOR_SIZE as i64 + (disk_region.size as i64);
        if this_sector > max_sector {
            max_sector = this_sector;
        }
    }

    Ok(
        [
            file_count,
            total_size,
            img.seek(SeekFrom::Start(0))? as i64,
            max_sector
        ]
    )
}

pub fn unpack(
    img_path: &str,
    target_dir: &str,
    progress_sender: Sender<String>,
) -> anyhow::Result<()> {
    let target_dir = PathBuf::from(target_dir);

    let mut img = open_image_raw(Path::new(img_path))?;
    let volume = xdvdfs::read::read_volume(&mut img)?;
    let tree = volume.root_table.file_tree(&mut img)?;

    for (dir, dirent) in &tree {
        if INTERRUPTED.load(Relaxed) {
            bail!("Interrupted");
        }

        let dir = &dir
            .trim_start_matches('/')
            .split('/')
            .collect::<Vec<_>>()
            .join(MAIN_SEPARATOR_STR);
        let dirname = target_dir.join(dir);
        let file_name = dirent.name_str::<std::io::Error>()?;
        let file_path = dirname.join(&*file_name);
        let is_dir = dirent.node.dirent.is_directory();

        if is_dir {
            progress_sender.send(format!(
                "Unpacked dir: {:?} from sector {}",
                file_path.clone(),
                dirent.node.dirent.data.sector as u64
            ))?;
        } else {
            progress_sender.send(format!(
                "Unpacked file: {:?} from sector {}",
                file_path.clone(),
                dirent.node.dirent.data.sector as u64
            ))?;
        }

        std::fs::create_dir_all(dirname)?;
        if dirent.node.dirent.is_directory() {
            std::fs::create_dir(file_path)?;
            continue;
        }

        if dirent.node.dirent.filename_length == 0 {
            eprintln!("WARNING: {:?} has an empty file name, skipping", file_path);
            continue;
        }

        let mut file = File::options()
            .write(true)
            .truncate(true)
            .create(true)
            .open(file_path)?;

        if dirent.node.dirent.is_empty() {
            continue;
        }

        dirent.node.dirent.seek_to(&mut img)?;
        let data = img.get_ref().get_ref().try_clone();
        match data {
            Ok(data) => {
                let data = data.take(dirent.node.dirent.data.size as u64);
                let mut data = std::io::BufReader::new(data);
                std::io::copy(&mut data, &mut file)?;
            }
            Err(err) => {
                eprintln!("Error in fast path, falling back to slow path: {:?}", err);
                let data = dirent.node.dirent.read_data_all(&mut img)?;
                file.write_all(&data)?;
            }
        }
    }

    Ok(())
}

pub fn unpack_file(
    img_path: &str,
    output_file: &str,
    search_file: &str,
) -> Result<(), anyhow::Error> {
    let target_file = PathBuf::from(output_file);
    let search_path = PathBuf::from(search_file.to_lowercase());

    let mut img = open_image_raw(Path::new(img_path))?;
    let volume = xdvdfs::read::read_volume(&mut img)?;
    let tree = volume.root_table.file_tree(&mut img)?;

    for (dir, dirent) in &tree {
        let file_name = dirent.name_str::<std::io::Error>()?;

        let internal_path = PathBuf::from(
            format!(
                "{}/{}",
                if dir == "/" { "" } else { dir },
                file_name.as_ref()
            )
            .to_lowercase(),
        );

        if internal_path != search_path {
            continue;
        }

        let is_dir = dirent.node.dirent.is_directory();

        if is_dir {
            return Err(anyhow!(
                "Search path is a folder, but only files are supported"
            ));
        }

        if dirent.node.dirent.filename_length == 0 {
            return Err(anyhow!(
                "Empty filename length for this search file. Perhaps the image is corrupt"
            ));
        }

        let mut file = File::options()
            .write(true)
            .truncate(true)
            .create(true)
            .open(target_file)?;

        if dirent.node.dirent.is_empty() {
            return Ok(());
        }

        dirent.node.dirent.seek_to(&mut img)?;
        let data = img.get_ref().get_ref().try_clone();
        match data {
            Ok(data) => {
                let data = data.take(dirent.node.dirent.data.size as u64);
                let mut data = std::io::BufReader::new(data);
                std::io::copy(&mut data, &mut file)?;
            }
            Err(err) => {
                eprintln!("Error in fast path, falling back to slow path: {:?}", err);
                let data = dirent.node.dirent.read_data_all(&mut img)?;
                file.write_all(&data)?;
            }
        }

        return Ok(());
    }

    Err(anyhow!(ErrorKind::NotFound))
}

pub fn file_offset(
    img_path: &str,
    search_file: &str
) -> anyhow::Result<[i64; 2]> {
    let search_path = PathBuf::from(search_file.to_lowercase());

    let mut img = open_image_raw(Path::new(img_path))?;
    let volume = xdvdfs::read::read_volume(&mut img)?;
    let tree = volume.root_table.file_tree(&mut img)?;

    for (dir, dirent) in &tree {
        let file_name = dirent.name_str::<std::io::Error>()?;

        let internal_path = PathBuf::from(
            format!(
                "{}/{}",
                if dir == "/" { "" } else { dir },
                &file_name
            ).to_lowercase(),
        );

        if internal_path != search_path {
            continue;
        }

        let is_dir = dirent.node.dirent.is_directory();

        if is_dir {
            bail!(
                "Search path is a folder, but only files are supported"
            );
        }

        if dirent.node.dirent.filename_length == 0 {
            bail!(
                "Empty filename length for this search file. Perhaps the image is corrupt"
            );
        }

        dirent.node.dirent.seek_to(&mut img)?;
        return Ok(
            [
                img.seek(SeekFrom::Current(0))? as i64,
                dirent.node.dirent.data.size as i64
            ]
        )
    }

    Err(anyhow!(ErrorKind::NotFound))
}