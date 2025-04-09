use ciso::write::AsyncWriter;
use std::io::{Error, Seek, SeekFrom};
use std::{
    collections::BTreeMap,
    fs::File,
    io::{self, Write},
    path::PathBuf,
};

const FILE_SPLIT_POINT: u64 = 0xffbf6000;

pub struct SplitOutput {
    file_name: PathBuf,
    splits: BTreeMap<u64, File>,
    position: u64,
}

impl SplitOutput {
    pub fn new(file_name: PathBuf) -> Self {
        Self {
            file_name,
            splits: BTreeMap::new(),
            position: 0,
        }
    }

    fn split_name(&self, index: u64) -> PathBuf {
        self.file_name
            .with_extension(format!("{}.cso", index + 1))
    }

    fn handle_for_position(&mut self, position: u64) -> io::Result<&mut File> {
        let index = position / FILE_SPLIT_POINT;

        if self.splits.contains_key(&index) {
            return Ok(self.splits.get_mut(&index).unwrap());
        }

        let file = self.split_name(index);
        let file = File::create(file)?;
        self.splits.insert(index, file);
        Ok(self.splits.get_mut(&index).unwrap())
    }

    fn atomic_write(&mut self, position: u64, data: &[u8]) -> Result<(), Error> {
        let mut written = 0;

        while written < data.len() {
            let handle = self.handle_for_position(position + written as u64)?;
            let bytes_to_split = (position + written as u64) % FILE_SPLIT_POINT;
            let bytes_to_split = if bytes_to_split == 0 {
                FILE_SPLIT_POINT
            } else {
                bytes_to_split
            };
            let to_write = core::cmp::min((data.len() - written) as u64, bytes_to_split);
            assert_ne!(to_write, 0);

            handle.atomic_write(
                position + written as u64,
                &data[written..(written + to_write as usize)],
            )?;
            written += to_write as usize;
        }

        Ok(())
    }

    pub fn close(self) {
        // all files will be closed when dropped
    }
}

impl Write for SplitOutput {
    fn write(&mut self, mut buf: &[u8]) -> io::Result<usize> {
        self.atomic_write(self.position, &mut buf)?;
        Ok(buf.len())
    }

    fn flush(&mut self) -> io::Result<()> {
        for handle in self.splits.values_mut() {
            handle.flush()?;
        }
        Ok(())
    }
}

impl Seek for SplitOutput {
    fn seek(&mut self, pos: SeekFrom) -> io::Result<u64> {
        self.position = match pos {
            SeekFrom::Start(p) => p,
            SeekFrom::End(_) => {
                return Err(Error::new(
                    io::ErrorKind::Unsupported,
                    "SeekFrom::End not supported",
                ))
            }
            SeekFrom::Current(offset) => (self.position as i64 + offset) as u64,
        };
        Ok(self.position)
    }
}
