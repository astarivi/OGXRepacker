package ovh.astarivi.xboxlib.core.naming;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ovh.astarivi.xboxlib.core.storage.OGXArchive;
import ovh.astarivi.xboxlib.core.storage.PersistenceRepository;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.core.xbe.XBE;

import java.nio.file.Path;


public class OGXName {
    public static @Nullable OGXArchive.Game getOGXGameForXbe(@NotNull XBE xbe) {
        return PersistenceRepository.getInstance().getGamesArchive().get(
                xbe.cert.dwTitleId
        );
    }

    public static OGXArchive.Game getForGame(XBE xbe, Path entry, boolean isArchiveEnabled) {
        OGXArchive.Game game = new OGXArchive.Game();
        game.title_id = xbe.cert.dwTitleId;
        String entryFilename = Utils.toFatXFilename(
                Utils.removeFileExtension(
                        entry.getFileName().toString(),
                        true
                )
        );
        game.title = entryFilename.substring(0, Math.min(entryFilename.length(), 42));
        game.iso_name = entryFilename.substring(0, Math.min(entryFilename.length(), 36));

        if (!isArchiveEnabled) return game;

        OGXArchive.Game archiveGame = getOGXGameForXbe(xbe);

        if (archiveGame != null) {
            return archiveGame;
        }

        return game;
    }
}
