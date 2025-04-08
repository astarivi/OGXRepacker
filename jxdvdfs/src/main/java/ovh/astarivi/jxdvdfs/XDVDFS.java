package ovh.astarivi.jxdvdfs;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.base.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Getter
@Setter
@SuppressWarnings("unused")
public class XDVDFS {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private static boolean libraryLoaded = false;
    private XDVDFSListener packListener = null;
    private XDVDFSListener unpackListener = null;

    private native void pack(String source, String destination) throws XDVDFSException;
    private native void packSplit(String source, String destination, long partSizeBytes) throws XDVDFSException;
    private native void unpack(String source, String destination) throws XDVDFSException;
    private native void ciso(String source, String destination, long partSizeBytes, boolean rebuild) throws XDVDFSException;
    private native void ufile(String source, String destination, String internalSearch) throws XDVDFSException;
    private native long[] stat(String source) throws XDVDFSException;
    private native long[] fstat(String source, String internalSearch) throws XDVDFSException;

    public XDVDFS() {
        if (!libraryLoaded) {
            NativeLoader.load("xdvdfs_jlib");
            libraryLoaded = true;
        }
    }

    public XDVDFS(String loadFrom) {
        if (!libraryLoaded) {
            System.load(loadFrom);
            libraryLoaded = true;
        }
    }

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
        this.pack(
                input.toAbsolutePath().toString(),
                output.toAbsolutePath().toString()
        );
    }

    /**
     * Packs a directory, or an image, to a XDVDFS XISO image.
     * <p>
     * Packs the input file (only XISO images are allowed) or
     * directory to a XDVDFS XISO image, and splits the
     * result.
     *
     * @param input The file or directory to convert to XISO.
     * @param output The path to save the resulting image to.
     * @param splitSize The size of each part, in bytes
     */
    public void packSplit(@NotNull Path input, @NotNull Path output, long splitSize) throws XDVDFSException {
        this.packSplit(
                input.toAbsolutePath().toString(),
                output.toAbsolutePath().toString(),
                splitSize
        );
    }

    /**
     * Compresses a directory, or an image, to a CISO image.
     * <p>
     * Packs the input file (only XISO images are allowed) or
     * directory to a XDVDFS XISO CISO image.
     * This operation removes the initial offset (if any) in the
     * input file, regardless of rebuild value.
     *
     * @param input The file or directory to convert to XISO.
     * @param output The path to save the resulting image to.
     * @param splitSize The size of each part, in bytes
     * @param rebuild True to fully rebuild the XISO image. Must be true for directories.
     */
    public void compressCISO(@NotNull Path input, @NotNull Path output, long splitSize, boolean rebuild) throws XDVDFSException, IllegalArgumentException {
        Path inputAbsolute = input.toAbsolutePath();

        if (rebuild && Files.isDirectory(inputAbsolute)) {
            throw new IllegalArgumentException("This input is a directory, but rebuild was set to false");
        }

        this.ciso(
                inputAbsolute.toString(),
                output.toAbsolutePath().toString(),
                splitSize,
                rebuild
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
    public XDVDFSStat stat(Path image) throws IOException, XDVDFSException {
        Path realPath = image.toRealPath();

        if (!realPath.toString().toLowerCase().endsWith(".iso")) {
            throw new IllegalArgumentException(".iso extension expected for image param");
        }

        long[] result = this.stat(realPath.toString());

        return new XDVDFSStat(result[0], result[1], result[2], result[3]);
    }

    /**
     * Unpacks the image to the given output directory.
     * <p>
     * Unpacks the given XISO image to the given output folder
     * path. If the output folder does not exist, it is created.
     *
     * @param input Path to an .iso (XISO) image.
     * @param output Path to the output folder
     */
    public void unpack(@NotNull Path input, @NotNull Path output) throws IOException, XDVDFSException {
        Path realInput = input.toRealPath();
        Path realOutput = output.toAbsolutePath();

        Files.createDirectories(realOutput);

        if (!Files.isDirectory(realOutput)) {
            throw new IOException("Couldn't create output folder with the given path");
        }

        if (!realInput.toString().toLowerCase().endsWith(".iso")) {
            throw new IllegalArgumentException(".iso extension expected for input param");
        }

        this.unpack(
                realInput.toString(),
                realOutput.toString()
        );
    }

    /**
     * Unpacks a single file contained within the image to the
     * output path, which is interpreted as a file.
     * <p>
     * Unpacks the given search path within the image to the
     * given output path.
     *
     * @param image Path to an .iso (XISO) image.
     * @param output Path for the output file (must be a file, not a folder)
     * @param searchFile Path to a file within the image, ex: "/default.xbe". Case-insensitive.
     */
    public void unpackFile(
            @NotNull Path image,
            @NotNull Path output,
            @NotNull String searchFile
    ) throws IOException, XDVDFSException {
        Path realImage = image.toRealPath();
        Path realOutput = output.toAbsolutePath();

        if (searchFile.isBlank()) {
            throw new IllegalArgumentException("searchFile param cannot be blank");
        }

        ufile(
                realImage.toString(),
                realOutput.toString(),
                searchFile
        );
    }


    /**
     * Searches for a single file contained within the volume, and
     * returns the offset (all other offset inclusive), and reported size.
     *
     * @param image Path to an .iso (XISO) image.
     * @param searchFile Path to a file within the image, ex: "/default.xbe". Case-insensitive.
     */
    public XDVDFSFile fileStat(
            @NotNull Path image,
            @NotNull String searchFile
    ) throws IOException, XDVDFSException {
        Path realImage = image.toRealPath();

        if (searchFile.isBlank()) {
            throw new IllegalArgumentException("searchFile param cannot be blank");
        }

        long[] result = fstat(
                realImage.toString(),
                searchFile
        );

        return new XDVDFSFile(result[0], result[1]);
    }
}