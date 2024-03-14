use maybe_async::maybe_async;
use std::path::PathBuf;
use std::sync::mpsc::Sender;
use xdvdfs::blockdev::BlockDeviceWrite;
use xdvdfs::write::{self, img::ProgressInfo};

#[maybe_async]
pub async fn pack<H: BlockDeviceWrite<std::io::Error> + std::io::Write + std::io::Seek>(
    source_path: PathBuf,
    target_image: &mut H,
    progress_sender: Sender<String>,
) -> Result<(), anyhow::Error> {
    let progress_callback = |pi| match pi {
        ProgressInfo::DirAdded(path, sector) => {
            progress_sender
                .send(format!("Added dir: {:?} at sector {}", path, sector))
                .unwrap();
        }
        ProgressInfo::FileAdded(path, sector) => {
            progress_sender
                .send(format!("Added file: {:?} at sector {}", path, sector))
                .unwrap();
        }
        _ => {}
    };

    let meta = std::fs::metadata(&source_path)?;

    if meta.is_dir() {
        write::img::create_xdvdfs_image(
            &source_path,
            &mut write::fs::StdFilesystem,
            target_image,
            progress_callback,
        )
        .await?;
    } else if meta.is_file() {
        let source = super::img::open_image_raw(&source_path).await?;

        let mut fs = write::fs::XDVDFSFilesystem::new(source)
            .await
            .ok_or(anyhow::anyhow!("Failed to create XDVDFS filesystem"))?;

        write::img::create_xdvdfs_image(
            &PathBuf::from("/"),
            &mut fs,
            target_image,
            progress_callback,
        )
        .await?;
    } else {
        return Err(anyhow::anyhow!("Symlink image sources are not supported"));
    }

    Ok(())
}

// #[cfg(test)]
// mod test {
//     use super::*;
//     #[test]
//     fn test() {
//         let progress_callback = |pi| match pi {
//             ProgressInfo::DirAdded(path, sector) => {
//                 println!("Added dir: {:?} at sector {}", path, sector);
//             }
//             ProgressInfo::FileAdded(path, sector) => {
//                 println!("Added file: {:?} at sector {}", path, sector);
//             }
//             _ => {}
//         };
//
//         let _result = pack(
//             PathBuf::from("D:\\xbox\\test\\redump.iso"),
//             PathBuf::from("D:\\xbox\\test\\output.iso"),
//             619430400,
//             progress_callback
//         );
//     }
// }
