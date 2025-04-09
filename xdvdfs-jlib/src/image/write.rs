use crate::image::img::{open_image_raw, BufFileSectorLinearFs};
use crate::image::wrapper::SplitBufReader;
use crate::java::INTERRUPTED;
use anyhow::bail;
use ciso::write::{AsyncWriter, CSOCreationError, SectorReader};
use std::io::{Seek, Write};
use std::path::PathBuf;
use std::sync::atomic::Ordering::Relaxed;
use std::sync::mpsc::Sender;
use xdvdfs::blockdev::BlockDeviceWrite;
use xdvdfs::util;
use xdvdfs::write::fs;
use xdvdfs::write::fs::{CisoSectorInput, SectorLinearBlockDevice, SectorLinearBlockFilesystem, StdFilesystem, XDVDFSFilesystem};
use xdvdfs::write::img::{create_xdvdfs_image, ProgressInfo};

pub fn pack<H: BlockDeviceWrite<std::io::Error> + Write + Seek>(
    source_path: &PathBuf,
    target_image: &mut H,
    progress_sender: &Sender<String>,
) -> anyhow::Result<()> {
    let meta = std::fs::metadata(source_path)?;

    if meta.is_dir() {
        let mut fs = StdFilesystem::create(source_path);

        generic_pack(&mut fs, target_image, progress_sender)?;
    } else if meta.is_file() {
        let source = open_image_raw(source_path)?;

        let mut fs = XDVDFSFilesystem::new(source)
            .ok_or(anyhow::anyhow!("Failed to create XDVDFS filesystem"))?;

        generic_pack(&mut fs, target_image, progress_sender)?;
    } else {
        bail!("This type of file is not supported")
    }

    Ok(())
}

pub fn ciso<E, O: AsyncWriter<E>>(
    source_path: &PathBuf,
    target_image: &mut O,
    rebuild: bool,
    progress_sender: &Sender<String>,
) -> anyhow::Result<()> where O: Write + Seek {
    let meta = std::fs::metadata(source_path)?;

    if rebuild {
        if meta.is_dir() {
            let mut fs = StdFilesystem::create(&source_path);
            let mut slbd = SectorLinearBlockDevice::default();
            let mut slbfs: SectorLinearBlockFilesystem<
                std::io::Error,
                std::fs::File,
                StdFilesystem,
            > = SectorLinearBlockFilesystem::new(&mut fs);

            generic_pack(&mut slbfs, &mut slbd, progress_sender)?;

            let mut input = CisoSectorInput::new(slbd, slbfs);
            ciso_compress(&mut input, target_image, progress_sender)?;
        } else if meta.is_file(){
            let source = open_image_raw(&source_path)?;

            let mut fs = XDVDFSFilesystem::new(source)
                .ok_or(anyhow::anyhow!("Failed to create XDVDFS filesystem"))?;

            let mut slbd = SectorLinearBlockDevice::default();
            let mut slbfs: BufFileSectorLinearFs = SectorLinearBlockFilesystem::new(&mut fs);

            generic_pack(&mut slbfs, &mut slbd, progress_sender)?;

            let mut input = CisoSectorInput::new(slbd, slbfs);
            ciso_compress(&mut input, target_image, progress_sender)?;
        } else {
            bail!("This type of file is not supported");
        }
    } else {
        let mut source = SplitBufReader::new(source_path)?;

        ciso_compress(&mut source, target_image, progress_sender)?;
    }

    Ok(())
}

fn ciso_compress<E, I: SectorReader<E>, O: AsyncWriter<E>>(
    input_image: &mut I,
    target_image: &mut O,
    progress_sender: &Sender<String>
) -> Result<(), CSOCreationError<E>> {
    let mut total_sectors = 0;
    let mut sectors_finished = 0;
    let mut last_report = 0;

    let progress_callback_compression = |pi| match pi {
        ciso::write::ProgressInfo::SectorCount(c) => {
            total_sectors = c;
            progress_sender
                .send(format!("Found {} sectors to compress", c))
                .unwrap();
        },
        ciso::write::ProgressInfo::SectorFinished => {
            sectors_finished += 1;
            last_report += 1;

            if last_report > 5_000 {
                if INTERRUPTED.load(Relaxed) {
                    panic!("Interrupted");
                }
                last_report = 0;
                progress_sender
                    .send(format!("Compressing sectors ({}/{})", sectors_finished, total_sectors))
                    .unwrap();
            }
        }
        ciso::write::ProgressInfo::Finished => {
            progress_sender
                .send(format!("Compressing sectors ({}/{})", total_sectors, total_sectors))
                .unwrap();
        },
        _ => {}
    };

    ciso::write::write_ciso_image(input_image, target_image, progress_callback_compression)
}

fn generic_pack<H: BlockDeviceWrite<HE>, HE, FE: From<HE>> (
    from: &mut (impl fs::Filesystem<H, FE, HE> + ?Sized),
    to: &mut H,
    progress_sender: &Sender<String>
) -> Result<(), util::Error<FE>> {
    let progress_callback = |pi| match pi {
        ProgressInfo::DirAdded(path, sector) => {
            progress_sender
                .send(format!("Packed dir: {:?} at sector {}", path, sector))
                .unwrap();
        }
        ProgressInfo::FileAdded(path, sector) => {
            if INTERRUPTED.load(Relaxed) {
                panic!("Interrupted");
            }
            progress_sender
                .send(format!("Packed file: {:?} at sector {}", path, sector))
                .unwrap();
        }
        _ => {}
    };

    create_xdvdfs_image(
        from,
        to,
        progress_callback
    )
}