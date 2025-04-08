use std::{fs::File, io::BufReader, path::Path};
use xdvdfs::blockdev::OffsetWrapper;
use xdvdfs::write::fs::{SectorLinearBlockFilesystem, XDVDFSFilesystem};

pub type BufFileSectorLinearFs<'a> = SectorLinearBlockFilesystem<
    'a,
    std::io::Error,
    File,
    XDVDFSFilesystem<
        std::io::Error,
        OffsetWrapper<BufReader<File>, std::io::Error>,
    >,
>;

pub fn open_image_raw(
    path: &Path,
) -> Result<OffsetWrapper<BufReader<File>, std::io::Error>, anyhow::Error> {
    let img = File::options().read(true).open(path)?;
    let img = BufReader::new(img);
    Ok(OffsetWrapper::new(img)?)
}