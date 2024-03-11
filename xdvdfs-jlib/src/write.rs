use maybe_async::maybe_async;
use std::path::PathBuf;
use xdvdfs::write::{self, img::ProgressInfo};

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
