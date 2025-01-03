package ovh.astarivi.xboxlib.core;

import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.XDVDFS;
import ovh.astarivi.jxdvdfs.base.XDVDFSException;
import ovh.astarivi.jxdvdfs.base.XDVDFSStat;
import ovh.astarivi.xboxlib.core.attacher.Attacher;
import ovh.astarivi.xboxlib.core.naming.GameName;
import ovh.astarivi.xboxlib.core.pack.Packer;
import ovh.astarivi.xboxlib.core.split.SplitUtils;
import ovh.astarivi.xboxlib.core.storage.OGXArchive;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.core.utils.XDVDFSHelper;
import ovh.astarivi.xboxlib.core.xbe.XBE;
import ovh.astarivi.xboxlib.gui.ProgressForm;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import javax.swing.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;


public class Pack implements Runnable {
    // We are a bit under the actual FATX limit to avoid transfer issues
    public final static long FATX_LIMIT = 4_280_000_000L;
    private final GuiConfig config;
    private final ProgressForm progressForm;

    public Pack(GuiConfig config, ProgressForm progressForm) {
        this.config = config;
        this.progressForm = progressForm;
    }

    private void addEventNow(String message) {
        SwingUtilities.invokeLater(() -> progressForm.addEvent(message));
    }

    @Override
    public void run() {
        SwingUtilities.invokeLater(() -> {
            progressForm.getTotalProgress().setIndeterminate(true);
            progressForm.getCurrentProgress().setIndeterminate(true);
            progressForm.addEvent("Scanning input folder for files/folders to process");
        });

        ArrayList<Path> inputItems = new ArrayList<>();

        try(Stream<Path> walkStream = Files.list(config.inputField())) {
            walkStream
                    .filter(path -> {
                        // ISO file
                        if (Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".iso")) {
                            return true;
                        }

                        // Dir
                        return Files.isDirectory(path) && Utils.containsDefaultXbe(path);
                    })
                    .forEach(inputItems::add);
        } catch (IOException e) {
            Logger.error("Failed to walk path {}", config.inputField().toString());
            Logger.error(e);
            SwingUtilities.invokeLater(() -> progressForm.processError("Failed to walk input path"));
            return;
        }

        if (inputItems.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                progressForm.addEvent("No valid inputs found");
                progressForm.getTotalProgress().setIndeterminate(false);
                progressForm.getCurrentProgress().setIndeterminate(false);
                progressForm.getTotalProgress().setValue(100);
                progressForm.finish();
            });
            return;
        }

        SwingUtilities.invokeLater(() -> progressForm.getTotalProgress().setIndeterminate(false));

        XDVDFS xdvdfs;
        try {
            xdvdfs = new XDVDFS();
        } catch(Exception e) {
            Logger.error("Failed to initialize native XDVDFS!");
            Logger.error(e);
            SwingUtilities.invokeLater(() ->
                    progressForm.processError("Failed to initialize native XDVDFS. Check logs for more information")
            );
            return;
        }

        int totalEntries = inputItems.size();
        int currentEntry = 0;

        addEventNow("Found %d valid entries to process".formatted(totalEntries));

        for (Path entry : inputItems) {
            SwingUtilities.invokeLater(() -> progressForm.getCurrentProgress().setValue(0));

            entry = entry.toAbsolutePath();
            currentEntry++;

            addEventNow("Processing entry no. %d, %s\nReading file metadata".formatted(
                    currentEntry, entry.getFileName().toString()
            ));

            // Stat
            XDVDFSStat entryStat;
            try {
                entryStat = XDVDFSHelper.getStatFor(entry, xdvdfs);
            } catch (IOException e) {
                Logger.error("Error while calling stat for {}", entry);
                Logger.error(e);
                addEventNow("Error while fetching file metadata, skipping");
                continue;
            } catch (XDVDFSException e) {
                Logger.error("Native error while calling stat for {}", entry);
                Logger.error(e);
                addEventNow("Native error while fetching file metadata, skipping");
                continue;
            }

            addEventNow("Found %d files, totalling %dMB in effective size".formatted(
                    entryStat.fileCount(),
                    Math.floorDiv(entryStat.totalSize(), 1024 * 1024)
            ));

            // Get XBE
            XBE extractedXbe;
            try (RandomAccessFile xbeHandle = XDVDFSHelper.extractXBE(entry, xdvdfs)){
                extractedXbe = new XBE(xbeHandle);
            } catch (IOException e) {
                Logger.error("Error while reading XBE for {}", entry);
                Logger.error(e);
                addEventNow("Error while reading XBE, skipping");
                continue;
            } catch (XDVDFSException e) {
                Logger.error("Native error while reading XBE for {}", entry);
                Logger.error(e);
                addEventNow("Native error while reading XBE, skipping");
                continue;
            }

            // Naming
            OGXArchive.Game game = GameName.getForGame(
                    extractedXbe,
                    entry,
                    config.naming()
            );

            addEventNow("Entry identified as %s".formatted(game.title));

            Path currentOutputFolder = config.outputField().resolve(game.title);

            if (Files.isDirectory(currentOutputFolder)) {
                addEventNow("Output folder already exists %s, skipping".formatted(currentOutputFolder.toString()));
                continue;
            }

            try {
                Files.createDirectories(currentOutputFolder);
            } catch (IOException e) {
                addEventNow("Failed to create output folder %s, skipping".formatted(currentOutputFolder.toString()));
                continue;
            }

            SwingUtilities.invokeLater(() -> {
                progressForm.getCurrentProgress().setIndeterminate(false);
                progressForm.getCurrentProgress().setValue(0);
            });

            // This is dumb, but I've used this kind of dumb before
            final long[] extractedFiles = {0};
            xdvdfs.setPackListener(event ->
                SwingUtilities.invokeLater(() -> {
                    if (event.startsWith("Packed file:")) {
                        extractedFiles[0]++;
                    }

                    progressForm.addEvent(event);

                    int progress = (int) ((extractedFiles[0] / ((float) entryStat.fileCount())) * 100);

                    progressForm.getCurrentProgress().setValue(progress);
                })
            );

            xdvdfs.setUnpackListener(event ->
                    SwingUtilities.invokeLater(() -> {
                        if (event.startsWith("Unpacked file:")) {
                            extractedFiles[0]++;
                        }

                        progressForm.addEvent(event);

                        int progress = (int) ((extractedFiles[0] / ((float) entryStat.fileCount())) * 100);

                        progressForm.getCurrentProgress().setValue(progress);
                    })
            );

            Packer.PackProgress packerProgressTracker = percentage ->
                    SwingUtilities.invokeLater(() -> progressForm.getCurrentProgress().setValue(percentage));

            if(Thread.interrupted()) {
                return;
            }

            GuiConfig.Pack packMode = this.config.pack();

            if (packMode == GuiConfig.Pack.XDVDFS_AUTO) {
                packMode = OGXArchive.TRIM_ONLY_TITLES.contains(game.title_id) ? GuiConfig.Pack.XDVDFS_TRIM : GuiConfig.Pack.XDVDFS_REBUILD;
            }

            if (Files.isDirectory(entry)) {
                if (packMode == GuiConfig.Pack.EXTRACT) {
                    addEventNow("Skipping input. Folders cannot be extracted");
                    continue;
                }
                packMode = GuiConfig.Pack.XDVDFS_REBUILD;
            }

            Path packedImage = currentOutputFolder.resolve(game.iso_name + ".iso");
            // Pack
            try {
                switch (packMode) {
                    case XDVDFS_REBUILD -> {
                        if (config.split() != GuiConfig.Split.NO) {
                            xdvdfs.packSplit(
                                    entry,
                                    packedImage,
                                    config.split() == GuiConfig.Split.FATX ? FATX_LIMIT : (entryStat.totalSize() / 2) + 10_000_000
                            );

                            SplitUtils.rename(packedImage);
                        } else {
                            xdvdfs.pack(
                                    entry,
                                    packedImage
                            );
                        }
                    }
                    case XDVDFS_TRIM -> {
                        addEventNow("Using conservative, trimming packer. Current progress may not update until packing is finished");
                        Packer packer = new Packer(
                                entry,
                                packedImage,
                                config,
                                entryStat
                        );

                        packer.setListener(packerProgressTracker);

                        packer.conservativePack(true);
                    }
                    case XDVDFS_KEEP -> {
                        addEventNow("Using conservative packer. Current progress may not update until packing is finished");

                        Packer packer = new Packer(
                                entry,
                                packedImage,
                                config,
                                entryStat
                        );

                        packer.setListener(packerProgressTracker);

                        packer.conservativePack(false);
                    }
                    case EXTRACT -> {
                        xdvdfs.unpack(entry, packedImage.getParent());
                    }
                    // This case should never trigger
                    default -> {
                        Logger.error("Failed to choose best method for {}", entry);
                        addEventNow("Image not recognized, unable to choose best method, skipping...");
                        continue;
                    }
                }
            } catch (IOException e) {
                Logger.error("Failed to pack image {}", entry);
                Logger.error(e);
                addEventNow("Failed to pack image, skipping");
                continue;
            } catch (XDVDFSException e) {
                Logger.error("Native failure to pack image {}", entry);
                Logger.error(e);
                addEventNow("Native failure to pack image, skipping");
                continue;
            }

            if(Thread.interrupted()) {
                return;
            }

            addEventNow("Packing finished for %s".formatted(game.title));

            if (config.pack() == GuiConfig.Pack.EXTRACT) {
                continue;
            }

            int finalCurrentEntry = currentEntry;
            if (config.attacher() == GuiConfig.Attacher.NONE) {
                SwingUtilities.invokeLater(() ->  {
                    progressForm.getCurrentProgress().setValue(100);
                    int progress = (int) ((finalCurrentEntry / ((float) totalEntries)) * 100);
                    progressForm.getTotalProgress().setValue(progress);
                });
                continue;
            }

            addEventNow("Creating attacher");

            if(Thread.interrupted()) {
                return;
            }

            try {
                Attacher attacherManager = new Attacher(
                        extractedXbe,
                        game,
                        currentOutputFolder.resolve("default.xbe")
                );

                attacherManager.create(config.attacher(), config.naming());
            } catch (IOException e) {
                addEventNow("Error creating attacher, skipping...");
                continue;
            }

            addEventNow("Done");

            SwingUtilities.invokeLater(() -> {
                int progress = (int) ((finalCurrentEntry / ((float) totalEntries)) * 100);
                progressForm.getTotalProgress().setValue(progress);
            });
        }

        SwingUtilities.invokeLater(() -> {
            progressForm.getCurrentProgress().setIndeterminate(false);
            progressForm.getCurrentProgress().setValue(100);
            progressForm.getTotalProgress().setIndeterminate(false);
            progressForm.getTotalProgress().setValue(100);
            progressForm.addEvent("All done!");
            progressForm.finish();
        });
    }
}
