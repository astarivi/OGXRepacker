package ovh.astarivi.xboxlib.core.utils;

import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.XDVDFS;
import ovh.astarivi.jxdvdfs.base.XDVDFSException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class XDVDFSHelper {
    public static long[] getStatFor(Path input, XDVDFS instance) throws IOException, XDVDFSException {
        if (Files.isRegularFile(input)) {
            return instance.stat(input);
        }

        final long[] countAndSize = {0, 0};

        if (!Files.isDirectory(input)) {
            throw new IOException("This path seems to be incorrect. It's not a folder, nor a file.");
        }

        Files.walkFileTree(input, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                countAndSize[0]++;
                try {
                    countAndSize[1] += Files.size(file);
                } catch (IOException e) {
                    Logger.error("Failed to get file size for {}", file.toString());
                    Logger.error(e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Logger.error("Failed to visit file {}", file.toString());
                Logger.error(exc);
                return FileVisitResult.CONTINUE;
            }
        });

        return countAndSize;
    }

    public static void extractXBE(Path input, Path output, XDVDFS xdvdfs) throws IOException, XDVDFSException {
        if (Files.isDirectory(input)) {
            Files.copy(input.resolve("default.xbe"), output, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        xdvdfs.unpackFile(
                input,
                output,
                "/default.xbe"
        );
    }
}
