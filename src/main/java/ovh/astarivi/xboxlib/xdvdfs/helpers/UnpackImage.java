package ovh.astarivi.xboxlib.xdvdfs.helpers;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.xdvdfs.XDVDFSImage;
import ovh.astarivi.xboxlib.xdvdfs.base.UnpackImageListener;
import ovh.astarivi.xboxlib.xdvdfs.exceptions.XDVDFSException;
import ovh.astarivi.xboxlib.xdvdfs.layout.DirectoryEntry;
import ovh.astarivi.xboxlib.xdvdfs.layout.Disk;
import ovh.astarivi.xboxlib.xdvdfs.utils.Pair;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


public class UnpackImage {
    private final Path from;
    private final Path to;
    private UnpackImageListener listener = null;

    public UnpackImage(Path from, Path to) {
        this.from = from;
        this.to = to;

        // Although this should be checked beforehand, but not check twice
        if (!Files.isRegularFile(from)) {
            throw new IllegalArgumentException("The input doesn't exist or is not a file.");
        }

        if (!this.from.getFileName().toString().toLowerCase().endsWith(".iso")) {
            throw new IllegalArgumentException("The input doesn't have a .iso extension.");
        }

        if (Files.isRegularFile(to)) {
            throw new IllegalArgumentException("Output must be a folder. It's a file.");
        }
    }

    public void setListener(UnpackImageListener listener) {
        this.listener = listener;
    }

    public void process() throws IOException, XDVDFSException {
        if (listener != null) listener.onStep(UnpackImageListener.CurrentStep.FETCHING_ROOT_TREE);

        try (XDVDFSImage image = new XDVDFSImage(from.toFile(), "r")) {
            ByteBuffer buffer = ByteBuffer.allocate(XDVDFSImage.SECTOR_SIZE).order(ByteOrder.LITTLE_ENDIAN);

            image.seek(XDVDFSImage.SECTOR_SIZE * 32);
            image.getChannel().read(buffer);

            buffer.flip();

            Disk.VolumeDescriptor volumeDescriptor = new Disk.VolumeDescriptor(buffer);

            if (!volumeDescriptor.isValid()) {
                throw new XDVDFSException("Invalid XDVDFS (XISO) image, MAGIC doesn't match.");
            }

            if (listener != null) listener.onStep(UnpackImageListener.CurrentStep.WALKING_FILE_TREE);
            ArrayList<Pair<String, DirectoryEntry.EntryNode>> tree = volumeDescriptor.root_table.getFileTree(image);

            Files.createDirectories(to);

            if (listener != null) listener.onStep(UnpackImageListener.CurrentStep.EXTRACTING_FILES);

            float totalItems = (float) tree.size();
            int currentItem = 0;

            for (Pair<String, DirectoryEntry.EntryNode> pair : tree) {
                currentItem++;

                float currentProgress = ((float) currentItem / totalItems) * 100F;

                DirectoryEntry.DiskData diskData = pair.value().node.dirent;

                String currentFile = "%s/%s (%d bytes) %s".formatted(
                        pair.key(),
                        pair.value().getName(),
                        diskData.data.size,
                        diskData.attributes.directory ? "(folder)" : ""
                ).trim();

                if (listener != null) listener.onProgress(
                        (int) currentProgress,
                        currentFile
                );

                Logger.debug(currentFile);

                String parentPath;

                if (pair.key().startsWith("/") || pair.key().startsWith("\\")) {
                    parentPath = pair.key().substring(1);
                } else {
                    parentPath = pair.key();
                }

                Path fullFolderPath = to.resolve(parentPath);
                Path outputFilePath = fullFolderPath.resolve(pair.value().getName());

                Files.createDirectories(fullFolderPath);

                if (diskData.isDirectory()) {
                    Files.createDirectories(outputFilePath);
                    continue;
                }

                try(
                        FileOutputStream outputFile = new FileOutputStream(outputFilePath.toFile());
                        FileChannel outputChannel = outputFile.getChannel()
                ) {
                    image.getChannel().transferTo(
                            diskData.getOffset(image),
                            diskData.data.size,
                            outputChannel
                    );
                }
            }
        }

        if (listener != null) listener.onProgress(
                100,
                "All done!"
        );

        if (listener != null) listener.onFinished();
    }
}
