use std::path::PathBuf;
use std::sync::mpsc::Sender;
use anyhow::bail;
use xdvdfs::blockdev::BlockDeviceWrite;
use xdvdfs::write::fs::{StdFilesystem, XDVDFSFilesystem};
use xdvdfs::write::img::{create_xdvdfs_image, ProgressInfo};


pub fn pack<H: BlockDeviceWrite<std::io::Error> + std::io::Write + std::io::Seek>(
    source_path: &PathBuf,
    target_image: &mut H,
    progress_sender: Sender<String>,
) -> anyhow::Result<()> {
    let progress_callback = |pi| match pi {
        ProgressInfo::DirAdded(path, sector) => {
            progress_sender
                .send(format!("Packed dir: {:?} at sector {}", path, sector))
                .unwrap();
        }
        ProgressInfo::FileAdded(path, sector) => {
            progress_sender
                .send(format!("Packed file: {:?} at sector {}", path, sector))
                .unwrap();
        }
        _ => {}
    };

    let meta = std::fs::metadata(source_path)?;

    if meta.is_dir() {
        let mut fs = StdFilesystem::create(source_path);

        create_xdvdfs_image(
            &mut fs,
            target_image,
            progress_callback
        )?;
    } else if meta.is_file() {
        let source = super::img::open_image_raw(source_path)?;

        let mut fs = XDVDFSFilesystem::new(source)
            .ok_or(anyhow::anyhow!("Failed to create XDVDFS filesystem"))?;

        create_xdvdfs_image(
            &mut fs,
            target_image,
            progress_callback,
        )
        ?;
    } else {
        bail!("This type of file is not supported")
    }

    Ok(())
}
