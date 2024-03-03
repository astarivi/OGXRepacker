package ovh.astarivi.xboxlib.xdvdfs.layout;

import ovh.astarivi.xboxlib.xdvdfs.XDVDFSImage;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class Disk {
    public static class Region implements Cloneable {
        // u32
        public long sector;
        // u32
        public long size;

        public Region(ByteBuffer buffer) {
            sector = Integer.toUnsignedLong(buffer.getInt());
            size = Integer.toUnsignedLong(buffer.getInt());
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public long offset(long requested_offset) {
            if (requested_offset >= size) {
                throw new IllegalArgumentException("requested_offset of %d is bigger than total size %d".formatted(
                        requested_offset,
                        size)
                );
            }

            return XDVDFSImage.SECTOR_SIZE * sector + requested_offset;
        }

        @Override
        public Region clone() {
            try {
                return (Region) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class VolumeDescriptor {
        public final byte[] magic0 = new byte[20];
        public final DirectoryEntry.Table root_table;
        public final long filetime;
        public final byte[] unused = new byte[1992];
        public final byte[] magic1 = new byte[20];

        public VolumeDescriptor(ByteBuffer buffer) {
            buffer.get(magic0);
            root_table = new DirectoryEntry.Table(buffer);
            filetime = buffer.getLong();
            buffer.get(unused);
            buffer.get(magic1);
        }

        public boolean isValid() {
            return Arrays.equals(magic0, XDVDFSImage.MAGIC) && Arrays.equals(magic1, XDVDFSImage.MAGIC);
        }
    }
}
