use maybe_async::maybe_async;
use std::path::PathBuf;
use xdvdfs::write::{self, img::ProgressInfo};
use splitfile::SplitFile;
use super::wrapper::SplitBufWriterWrapper;

#[maybe_async]
pub async fn pack_img(
    source_path: PathBuf,
    target_path: PathBuf,
    progress_callback: impl Fn(ProgressInfo),
) -> Result<(), anyhow::Error> {
    let target_image = std::fs::File::options()
        .write(true)
        .truncate(true)
        .create(true)
        .open(target_path)?;

    let mut target_image = std::io::BufWriter::with_capacity(1024 * 1024, target_image);

    let meta = std::fs::metadata(&source_path)?;

    if meta.is_dir() {
        write::img::create_xdvdfs_image(
            &source_path,
            &mut write::fs::StdFilesystem,
            &mut target_image,
            progress_callback,
        )
        .await?;
    } else if meta.is_file() {
        let source = crate::img::open_image_raw(&source_path).await?;

        let mut fs = write::fs::XDVDFSFilesystem::new(source)
            .await
            .ok_or(anyhow::anyhow!("Failed to create XDVDFS filesystem"))?;

        write::img::create_xdvdfs_image(
            &PathBuf::from("/"),
            &mut fs,
            &mut target_image,
            progress_callback,
        )
        .await?;
    } else {
        return Err(anyhow::anyhow!("Symlink image sources are not supported"));
    }

    Ok(())
}

#[maybe_async]
pub async fn pack_split(
    source_path: PathBuf,
    target_path: PathBuf,
    volume_size: i64,
    progress_callback: impl Fn(ProgressInfo),
) -> Result<(), anyhow::Error> {
    let target_image = SplitFile::create(target_path, volume_size as u64)?;

    let mut target_image = SplitBufWriterWrapper(
        std::io::BufWriter::with_capacity(1024 * 1024, target_image)
    );

    let meta = std::fs::metadata(&source_path)?;

    if meta.is_dir() {
        write::img::create_xdvdfs_image(
            &source_path,
            &mut write::fs::StdFilesystem,
            &mut target_image,
            progress_callback,
        )
            .await?;
    } else if meta.is_file() {
        let source = crate::img::open_image_raw(&source_path).await?;

        let mut fs = write::fs::XDVDFSFilesystem::new(source)
            .await
            .ok_or(anyhow::anyhow!("Failed to create XDVDFS filesystem"))?;

        write::img::create_xdvdfs_image(
            &PathBuf::from("/"),
            &mut fs,
            &mut target_image,
            progress_callback,
        )
            .await?;
    } else {
        return Err(anyhow::anyhow!("Symlink image sources are not supported"));
    }

    Ok(())
}

#[cfg(test)]
mod test {
    use super::*;
    #[test]
    fn test() {
        let progress_callback = |pi| match pi {
            ProgressInfo::DirAdded(path, sector) => {
                println!("Added dir: {:?} at sector {}", path, sector);
            }
            ProgressInfo::FileAdded(path, sector) => {
                println!("Added file: {:?} at sector {}", path, sector);
            }
            _ => {}
        };

        let result = pack_split(
            PathBuf::from("D:\\xbox\\test\\redump.iso"),
            PathBuf::from("D:\\xbox\\test\\output.iso"),
            619430400,
            progress_callback
        );
    }
}