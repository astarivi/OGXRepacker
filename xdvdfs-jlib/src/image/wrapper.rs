use std::fs::File;
use splitfile::SplitFile;
use std::io::{BufReader, BufWriter, Read, Result, Seek, SeekFrom, Write};
use std::path::Path;
use xdvdfs::blockdev::{BlockDeviceWrite, OffsetWrapper};

pub struct SplitBufWriterWrapper(pub BufWriter<SplitFile>);

impl SplitBufWriterWrapper {
    // hippity hoppity your experimental feature is now my property
    pub fn len(&mut self) -> Result<u64> {
        let old_pos = self.stream_position()?;
        let len = self.seek(SeekFrom::End(0))?;

        if old_pos != len {
            self.seek(SeekFrom::Start(old_pos))?;
        }

        Ok(len)
    }
}

impl Seek for SplitBufWriterWrapper {
    fn seek(&mut self, pos: SeekFrom) -> Result<u64> {
        self.0.seek(pos)
    }
}

impl Write for SplitBufWriterWrapper {
    fn write(&mut self, buf: &[u8]) -> Result<usize> {
        self.0.write(buf)
    }

    fn flush(&mut self) -> Result<()> {
        self.0.flush()
    }
}

impl BlockDeviceWrite<std::io::Error> for SplitBufWriterWrapper {
    fn write(
        &mut self,
        offset: u64,
        buffer: &[u8],
    ) -> core::result::Result<(), std::io::Error> {
        self.seek(SeekFrom::Start(offset))?;
        self.write_all(buffer)?;

        Ok(())
    }

    fn len(&mut self) -> core::result::Result<u64, std::io::Error> {
        self.len()
    }
}

pub struct SplitBufReader {
    inner: BufReader<File>,
    offset: u64,
}

impl SplitBufReader {
    pub fn new(path: &Path) -> anyhow::Result<Self> {
        let mut img = File::options().read(true).open(path)?;

        let mut offset = OffsetWrapper::new(img.try_clone()?)?;
        let offset = offset.seek(SeekFrom::Start(0))?;

        img.seek(SeekFrom::Start(offset))?;

        Ok(Self {
            inner: BufReader::new(img),
            offset
        })
    }
}

impl Read for SplitBufReader {
    fn read(&mut self, buf: &mut [u8]) -> Result<usize> {
        std::io::Read::read(&mut self.inner, buf)
    }
}

impl Seek for SplitBufReader {
    fn seek(&mut self, pos: SeekFrom) -> Result<u64> {
        match pos {
            SeekFrom::Start(pos) => self.inner.seek(SeekFrom::Start(self.offset + pos)),
            pos => self.inner.seek(pos),
        }
    }
}