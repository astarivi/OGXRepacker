package ovh.astarivi.xboxlib.xdvdfs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import ovh.astarivi.xboxlib.xdvdfs.exceptions.XDVDFSException;


public class XDVDFSImage extends RandomAccessFile {
    public static final int[] XDVDFS_OFFSETS = new int[]{
            // XISO
            0,
            // Redump
            405798912
    };
    public static final int SECTOR_SIZE = 2048;
    public static final byte[] MAGIC = "MICROSOFT*XBOX*MEDIA".getBytes(StandardCharsets.US_ASCII);
    private int root_offset;

    public XDVDFSImage(String name, String mode) throws IOException, XDVDFSException {
        super(name, mode);

        verifyImage();
    }

    public XDVDFSImage(File file, String mode) throws IOException, XDVDFSException {
        super(file, mode);

        verifyImage();
    }


    @Override
    public void seek(long pos) throws IOException {
        super.seek(root_offset + pos);
    }

    public int getRootOffset() {
        return root_offset;
    }

    public void verifyImage() throws IOException, XDVDFSException {
        for (int offset : XDVDFS_OFFSETS) {
            this.seek(offset + (2048 * 32));

            final byte[] magic = new byte[20];

            this.read(magic);

            if (Arrays.equals(magic, MAGIC)) {
                // Just in case
                this.seek(0);
                root_offset = offset;
                return;
            }
        }

        this.close();
        throw new XDVDFSException("Invalid image, MAGIC not found.");
    }
}
