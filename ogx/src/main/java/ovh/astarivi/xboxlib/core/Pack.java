package ovh.astarivi.xboxlib.core;

import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.XDVDFS;
import ovh.astarivi.jxdvdfs.base.XDVDFSException;
import ovh.astarivi.xboxlib.core.attacher.Attacher;
import ovh.astarivi.xboxlib.core.naming.GameName;
import ovh.astarivi.xboxlib.core.split.SplitUtils;
import ovh.astarivi.xboxlib.core.storage.OGXArchive;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.core.utils.XDVDFSHelper;
import ovh.astarivi.xboxlib.core.xbe.XBE;
import ovh.astarivi.xboxlib.gui.ProgressForm;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;


public class Pack implements Runnable {
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
            try {
                Files.createDirectories(Utils.temporaryPath);
                Utils.cleanTemp();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        progressForm.processError("Failed to create or clear OGX temporary directory")
                );
                return;
            }

            SwingUtilities.invokeLater(() -> progressForm.getCurrentProgress().setValue(0));

            entry = entry.toAbsolutePath();
            currentEntry++;

            addEventNow("Processing entry no. %d, %s\nReading file metadata".formatted(
                    currentEntry, entry.getFileName().toString()
            ));

            // Stat
            long[] entryStat;
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

            addEventNow("Found %d files, totalling %dMB in size".formatted(
                    entryStat[0],
                    Math.floorDiv(entryStat[1], 1024 * 1024)
            ));

            // Extract XBE
            Path extractedXbePath = Utils.temporaryPath.resolve("default.xbe");
            try {
                XDVDFSHelper.extractXBE(entry, extractedXbePath, xdvdfs);
            } catch (IOException e) {
                Logger.error("Error while extracting XBE for {}", entry);
                Logger.error(e);
                addEventNow("Error while extracting XBE, skipping");
                continue;
            } catch (XDVDFSException e) {
                Logger.error("Native error while extracting XBE for {}", entry);
                Logger.error(e);
                addEventNow("Native error while extracting XBE, skipping");
                continue;
            }

            XBE extractedXbe;
            try {
                extractedXbe = new XBE(extractedXbePath);
            } catch (IOException e) {
                addEventNow("Error while reading extracted XBE, skipping");
                continue;
            }

            // Naming
            OGXArchive.Game game = GameName.getForGame(
                    extractedXbe,
                    entry,
                    config.naming() == GuiConfig.Naming.OGXREPACKER
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

            // This is dumb, but I've used this dumb before
            final long[] extractedFiles = {0};
            xdvdfs.setPackListener(event ->
                SwingUtilities.invokeLater(() -> {
                    if (event.startsWith("Added file:")) {
                        extractedFiles[0]++;
                    }

                    progressForm.addEvent(event);

                    int progress = (int) ((extractedFiles[0] / ((float) entryStat[0])) * 100);

                    progressForm.getCurrentProgress().setValue(progress);
                })
            );

            if(Thread.interrupted()) {
                return;
            }

            Path packedImage = currentOutputFolder.resolve(game.iso_name + ".iso");
            // Pack
            try {
                if (config.split() == GuiConfig.Split.HALF) {
                    xdvdfs.packSplit(
                            entry,
                            packedImage,
                            (entryStat[1] / 2) + 7000000
                    );

                    SplitUtils.rename(packedImage);
                } else {
                    xdvdfs.pack(
                            entry,
                            packedImage
                    );
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
