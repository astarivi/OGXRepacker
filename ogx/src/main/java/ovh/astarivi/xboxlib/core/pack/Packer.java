package ovh.astarivi.xboxlib.core.pack;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ovh.astarivi.jxdvdfs.base.XDVDFSStat;
import ovh.astarivi.xboxlib.core.Pack;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;


public class Packer {
    private final Path input;
    private final Path output;
    private final GuiConfig config;
    private final XDVDFSStat stat;
    @Getter
    @Setter
    private PackProgress listener;

    public Packer(Path input, Path output, GuiConfig config, XDVDFSStat stat) {
        this.input = input;
        this.output = output;
        this.config = config;
        this.stat = stat;
    }

    public void conservativePack(boolean trim) throws IOException {
//        long sourceSize = trim ? stat.maxSize() : Files.size(input);
//        long bytesToCopy = sourceSize - stat.startOffset();
        long bytesToCopy = trim ? stat.maxSize() : Files.size(input) - stat.startOffset();

        Path baseFolder = output.getParent();
        String baseFileName = Utils.removeFileExtension(
                output.getFileName().toString(),
                false
        );

        GuiConfig.Split splitType = config.split();

        // We don't need to split here, our input isn't big enough
        if (splitType == GuiConfig.Split.FATX && bytesToCopy <= Pack.FATX_LIMIT) {
            splitType = GuiConfig.Split.NO;
        }

        @NotNull Path outputPath1;
        @Nullable Path outputPath2;
        if (splitType == GuiConfig.Split.NO) {
            outputPath1 = output;
            outputPath2 = null;
        } else {
            outputPath1 = baseFolder.resolve(baseFileName + ".1.iso");
            outputPath2 = baseFolder.resolve(baseFileName + ".2.iso");
        }

        try (FileChannel sourceChannel = FileChannel.open(input, StandardOpenOption.READ);
             FileChannel firstFile = FileChannel.open(outputPath1, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            switch (splitType) {
                case FATX -> {
                    try (FileChannel secondFile = FileChannel.open(Objects.requireNonNull(outputPath2), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                        sourceChannel.transferTo(stat.startOffset(), Pack.FATX_LIMIT, firstFile);
                        if (listener != null) {
                            listener.onProgress(50);
                        }
                        sourceChannel.transferTo(stat.startOffset() + Pack.FATX_LIMIT, bytesToCopy - Pack.FATX_LIMIT, secondFile);
                    }
                }
                case HALF -> {
                    try (FileChannel secondFile = FileChannel.open(Objects.requireNonNull(outputPath2), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                        long halfSize = bytesToCopy / 2;
                        sourceChannel.transferTo(stat.startOffset(), halfSize, firstFile);
                        if (listener != null) {
                            listener.onProgress(50);
                        }
                        sourceChannel.transferTo(stat.startOffset() + halfSize, bytesToCopy - halfSize, secondFile);
                    }
                }
                case NO -> sourceChannel.transferTo(stat.startOffset(), bytesToCopy, firstFile);
            }
        } finally {
            if (listener != null) {
                listener.onProgress(100);
            }
        }

    }

    public interface PackProgress{
        void onProgress(int percentage);
    }
}
