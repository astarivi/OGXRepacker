use ciso::read::CSOReader;
use std::{fs::File, io::BufReader, path::Path};
use xdvdfs::blockdev::{BlockDeviceRead, OffsetWrapper};

pub struct CSOBlockDevice<R: ciso::read::Read<std::io::Error>> {
    inner: CSOReader<std::io::Error, R>,
}

impl<R> BlockDeviceRead<std::io::Error> for CSOBlockDevice<R>
where
    R: ciso::read::Read<std::io::Error>,
{
    fn read(&mut self, offset: u64, buffer: &mut [u8]) -> Result<(), std::io::Error> {
        self.inner
            .read_offset(offset, buffer)
            .map_err(|e| match e {
                ciso::layout::Error::Other(e) => e,
                e => std::io::Error::new(std::io::ErrorKind::Other, e),
            })
    }
}

pub fn open_image_raw(
    path: &Path,
) -> Result<OffsetWrapper<BufReader<File>, std::io::Error>, anyhow::Error> {
    let img = File::options().read(true).open(path)?;
    let img = BufReader::new(img);
    Ok(OffsetWrapper::new(img)?)
}

// pub fn open_image(
//     path: &Path,
// ) -> Result<Box<dyn BlockDeviceRead<std::io::Error>>, anyhow::Error> {
//     if path.extension().is_some_and(|e| e == "cso") {
//         let file_base = path.with_extension("");
//         let split = file_base.extension().is_some_and(|e| e == "1");
//
//         let reader: Box<dyn ciso::read::Read<std::io::Error>> = if split {
//             let mut files = Vec::new();
//             for i in 1.. {
//                 let part = file_base.with_extension(format!("{}.cso", i));
//                 if !part.exists() {
//                     break;
//                 }
//
//                 let part = BufReader::new(File::open(part)?);
//                 files.push(part);
//             }
//
//             if files.is_empty() {
//                 return Err(anyhow::anyhow!("Failed to open file {:?}", path));
//             }
//
//             Box::from(ciso::split::SplitFileReader::new(files)?)
//         } else {
//             let file = BufReader::new(File::open(path)?);
//             Box::from(file)
//         };
//
//         let reader = CSOReader::new(reader)?;
//         let reader = Box::from(CSOBlockDevice { inner: reader });
//         Ok(reader)
//     } else {
//         let image = open_image_raw(path)?;
//         let image = Box::from(image);
//         Ok(image)
//     }
// }
