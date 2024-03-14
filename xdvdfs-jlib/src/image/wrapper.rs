use std::io::{BufWriter, Seek, SeekFrom, Write, Result};
use maybe_async::maybe_async;
use splitfile::SplitFile;
use xdvdfs::blockdev::BlockDeviceWrite;

pub struct SplitBufWriterWrapper(pub BufWriter<SplitFile>);

impl SplitBufWriterWrapper{
    // hippity hoppity your experimental feature is now my property
    pub fn len(&mut self) -> Result<u64>{
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

#[maybe_async]
impl BlockDeviceWrite<std::io::Error> for SplitBufWriterWrapper {
    async fn write(&mut self, offset: u64, buffer: &[u8]) -> core::result::Result<(), std::io::Error> {
        self.seek(SeekFrom::Start(offset))?;
        self.write_all(buffer)?;

        Ok(())
    }

    async fn len(&mut self) -> core::result::Result<u64, std::io::Error> {
        Ok(self.len()?)
    }
}
