package ovh.astarivi.xboxlib.xdvdfs.layout;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.xdvdfs.XDVDFSImage;
import ovh.astarivi.xboxlib.xdvdfs.exceptions.XDVDFSException;
import ovh.astarivi.xboxlib.xdvdfs.utils.StringDENPair;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;


public class Layout {

    public static class DiskRegion implements Cloneable{
        // u32
        public long sector;
        // u32
        public long size;

        public DiskRegion(ByteBuffer buffer) {
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
        public DiskRegion clone() {
            try {
                return (DiskRegion) super.clone();
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

        @Override
        public String toString() {
            return "VolumeDescriptor{" +
                    "magic0=" + Arrays.toString(magic0) +
                    ", root_table=" + root_table +
                    ", filetime=" + filetime +
                    ", unused=" + Arrays.toString(unused) +
                    ", magic1=" + Arrays.toString(magic1) +
                    '}';
        }
    }

    public static void main(String[] args) throws IOException, XDVDFSException {
        Path filePath = Paths.get("D:\\xbox\\test\\redump.iso");
        Path outputPath = Paths.get("D:\\xbox\\test\\output");

        try (XDVDFSImage image = new XDVDFSImage(filePath.toFile(), "r")) {
            ByteBuffer buffer = ByteBuffer.allocate(XDVDFSImage.SECTOR_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // Skip 32 sectors
            image.seek(XDVDFSImage.SECTOR_SIZE * 32);
            image.getChannel().read(buffer);

            buffer.flip();

            VolumeDescriptor vd = new VolumeDescriptor(buffer);

            Logger.info("Is VD valid? {}", vd.isValid());

            ArrayList<StringDENPair> tree = vd.root_table.getFileTree(image);

            Files.createDirectories(outputPath);

            for (StringDENPair pair : tree) {
                Logger.info("{}/{} ({} bytes)", pair.parent(), pair.node().getName(), pair.node().node.dirent.data.size);

                String parentPath;

                if (pair.parent().startsWith("/") || pair.parent().startsWith("\\")) {
                    parentPath = pair.parent().substring(1);
                } else {
                    parentPath = pair.parent();
                }

                Path fullFolderPath = outputPath.resolve(parentPath);
                Path outputFilePath = fullFolderPath.resolve(pair.node().getName());

                Files.createDirectories(fullFolderPath);

                if (pair.node().node.dirent.isDirectory()) {
                    Files.createDirectories(outputFilePath);
                    continue;
                }

                Logger.info(outputFilePath);

                try(
                        FileOutputStream outputFile = new FileOutputStream(outputFilePath.toFile());
                        FileChannel outputChannel = outputFile.getChannel()
                ) {
                    image.getChannel().transferTo(
                            pair.node().node.dirent.getOffset(image),
                            pair.node().node.dirent.data.size,
                            outputChannel
                    );
                }
            }
        }
    }
}
