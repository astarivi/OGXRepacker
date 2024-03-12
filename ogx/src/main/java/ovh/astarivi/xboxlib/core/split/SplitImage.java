package ovh.astarivi.xboxlib.core.split;

import lombok.Getter;
import lombok.Setter;
import ovh.astarivi.xboxlib.core.Threading;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;


public class SplitImage {
    private final Path inputPath;
    private final Path outputPath;
    @Getter
    @Setter
    private SplitProgress listener;
    private Future<?> listenerFuture;

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

            if (listener != null) {
                // Not the best code, but polling the files from .transferTo is the fastest way around this
                listenerFuture = Threading.getInstance().getInstantExecutor().submit(() -> {
                    while(true) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }

                        if (Thread.interrupted()) {
                            break;
                        }

                        if (!Files.isRegularFile(outputPath1)) continue;

                        long firstFileSize;
                        long secondFileSize;

                        try {
                            firstFileSize = Files.size(outputPath1);
                            secondFileSize = Files.size(outputPath2);
                        } catch (IOException | SecurityException e) {
                            continue;
                        }

                        if (firstFileSize < halfSize) {
                            listener.onProgress(
                                    (int) ((firstFileSize / (float) halfSize) * 50)
                            );

                            continue;
                        }

                        listener.onProgress(
                                50 + (int) ((secondFileSize / (float) halfSize) * 50)
                        );
                    }

                    listener.onProgress(100);
                });
            }

            inputStream.getChannel().transferTo(0, halfSize, outputStream1.getChannel());
            inputStream.getChannel().transferTo(halfSize, fileSize, outputStream2.getChannel());
        } finally {
            if (listenerFuture != null) {
                listenerFuture.cancel(true);
            }
            if (listener != null) {
                listener.onProgress(100);
            }
        }
    }

    public interface SplitProgress{
        void onProgress(int percentage);
    }
}