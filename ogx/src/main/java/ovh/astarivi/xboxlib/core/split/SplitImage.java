package ovh.astarivi.xboxlib.core.split;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class SplitImage {
    private final Path inputPath;
    private final Path outputPath;

    public SplitImage(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public void split() throws IOException {
        String inputFilename = inputPath.getFileName().toString();
        int lastDotIndex = inputFilename.lastIndexOf('.');

        if (lastDotIndex > 0) {
            inputFilename = inputFilename.substring(0, lastDotIndex);
        }

        Path outputPath1 = outputPath.resolve(inputFilename + ".1.iso");
        Path outputPath2 = outputPath.resolve(inputFilename + ".2.iso");

        try (FileInputStream inputStream = new FileInputStream(inputPath.toFile());
             FileOutputStream outputStream1 = new FileOutputStream(outputPath1.toFile());
             FileOutputStream outputStream2 = new FileOutputStream(outputPath2.toFile())) {

            long fileSize = inputStream.getChannel().size();
            long halfSize = fileSize / 2;

            inputStream.getChannel().transferTo(0, halfSize, outputStream1.getChannel());
            inputStream.getChannel().transferTo(halfSize, fileSize, outputStream2.getChannel());
        }
    }
}