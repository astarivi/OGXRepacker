use crate::img::{open_image, open_image_raw};
use anyhow::anyhow;
use maybe_async::maybe_async;
use std::fs::File;
use std::io::{ErrorKind, Read, Write};
use std::path::{Path, PathBuf, MAIN_SEPARATOR_STR};
use xdvdfs::write::img::ProgressInfo;

#[maybe_async]
pub async fn stat(img_path: &Path) -> Result<[i64; 2], anyhow::Error> {
    let mut img = open_image(img_path).await?;
    let volume = xdvdfs::read::read_volume(&mut img).await?;

    let tree = volume.root_table.file_tree(&mut img).await?;

    let mut total_size: i64 = 0;
    let mut file_count: i64 = 0;

    for (_dir, file) in &tree {
        if !file.node.dirent.is_directory() {
            total_size = total_size.wrapping_add(file.node.dirent.data.size() as i64);
            file_count += 1;
        }
    }

    Ok([file_count, total_size])
}

#[maybe_async]
pub async fn unpack_file(
    img_path: &str,
    output_file: &str,
    search_file: &str,
) -> Result<(), anyhow::Error> {
    let target_file = PathBuf::from(output_file);
    let search_path = PathBuf::from(search_file.to_lowercase());

    let mut img = open_image_raw(Path::new(img_path)).await?;
    let volume = xdvdfs::read::read_volume(&mut img).await?;
    let tree = volume.root_table.file_tree(&mut img).await?;

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
                let data = dirent.node.dirent.read_data_all(&mut img).await?;
                file.write_all(&data)?;
            }
        }

        return Ok(());
    }

    Err(anyhow!(ErrorKind::NotFound))
}

#[maybe_async]
pub async fn unpack(
    img_path: &str,
    target_dir: &str,
    progress_callback: impl Fn(ProgressInfo),
) -> Result<(), anyhow::Error> {
    let target_dir = PathBuf::from(target_dir);

    let mut img = open_image_raw(Path::new(img_path)).await?;
    let volume = xdvdfs::read::read_volume(&mut img).await?;
    let tree = volume.root_table.file_tree(&mut img).await?;

    for (dir, dirent) in &tree {
        let dir = &dir
            .trim_start_matches('/')
            .split('/')
            .collect::<Vec<_>>()
            .join(MAIN_SEPARATOR_STR);
        let dirname = target_dir.join(dir);
        let file_name = dirent.name_str::<std::io::Error>()?;
        let file_path = dirname.join(&*file_name);
        let is_dir = dirent.node.dirent.is_directory();

        let progress_info = match is_dir {
            true => {
                ProgressInfo::DirAdded(file_path.clone(), dirent.node.dirent.data.sector as u64)
            }
            false => {
                ProgressInfo::FileAdded(file_path.clone(), dirent.node.dirent.data.sector as u64)
            }
        };

        progress_callback(progress_info);

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
                let data = dirent.node.dirent.read_data_all(&mut img).await?;
                file.write_all(&data)?;
            }
        }
    }

    Ok(())
}
