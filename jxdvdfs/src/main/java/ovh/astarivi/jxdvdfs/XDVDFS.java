package ovh.astarivi.jxdvdfs;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.base.XDVDFSException;
import ovh.astarivi.jxdvdfs.base.XDVDFSListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Getter
@Setter
@SuppressWarnings("unused")
public class XDVDFS {
    static {
        System.loadLibrary("xdvdfs_jlib");
    }

    private XDVDFSListener packListener = null;
    private XDVDFSListener unpackListener = null;

    private native void pack(String source, String destination) throws XDVDFSException;
    private native void unpack(String source, String destination) throws XDVDFSException;
    private native int[] stat(String source) throws XDVDFSException;

    private void pack_callback(String message) {
        Logger.debug("Received event from XDVDFS pack: {}", message);
        if(packListener != null) packListener.onEvent(message);
    }

    private void unpack_callback(String message) {
        Logger.debug("Received event from XDVDFS unpack: {}", message);
        if(unpackListener != null) unpackListener.onEvent(message);
    }

    /**
     * Packs a directory, or an image, to a XDVDFS XISO image.
     * <p>
     * Packs the input file (only XISO images are allowed) or
     * directory to a XDVDFS XISO image.
     *
     * @param input The file or directory to convert to XISO.
     * @param output The path to save the resulting image to.
     */
    public void pack(@NotNull Path input, @NotNull Path output) throws IOException, XDVDFSException {
        Path realInput = input.toRealPath();
        Path realOutput = output.toRealPath();

        if (!Files.isRegularFile(realInput)) {
            throw new IOException("Input image does not exist, or is not readable");
        }

        this.pack(
                realInput.toString(),
                realOutput.toString()
        );
    }

    /**
     * Queries the image for its file count and real size.
     * <p>
     * Reads the XISO image entries for files and directories,
     * and counts the total files and real size of every file.
     *
     * @param image A path to an .iso (XISO) image.
     * @return Returns the file count, and the total size in bytes,
     * in that order.
     */
    public int[] stat(Path image) throws IOException, XDVDFSException {
        Path realPath = image.toRealPath();

        if (!realPath.endsWith(".iso")) {
            throw new IllegalArgumentException(".iso extension expected for image param");
        }

        if (!Files.isRegularFile(realPath)) {
            throw new IOException("Image does not exist, or is not readable");
        }

        return this.stat(realPath.toString());
    }

    public void unpack(@NotNull Path input, @NotNull Path output) throws IOException, XDVDFSException {
        Path realInput = input.toRealPath();
        Path realOutput = output.toRealPath();

        if (!Files.isRegularFile(realInput)) {
            throw new IOException("Input image does not exist, or is not readable");
        }

        this.unpack(
                realInput.toString(),
                realOutput.toString()
        );
    }
}