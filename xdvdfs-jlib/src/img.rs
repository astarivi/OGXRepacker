use maybe_async::maybe_async;
use std::{fs::File, io::BufReader, path::Path};
use xdvdfs::blockdev::{OffsetWrapper};


#[maybe_async]
pub async fn open_image_raw(
    path: &Path,
) -> Result<OffsetWrapper<BufReader<File>, std::io::Error>, anyhow::Error> {
    let img = File::options().read(true).open(path)?;
    let img = std::io::BufReader::new(img);
    Ok(xdvdfs::blockdev::OffsetWrapper::new(img).await?)
}
