package ovh.astarivi.xboxlib.core.naming;

import ovh.astarivi.xboxlib.core.storage.OGXArchive;
import ovh.astarivi.xboxlib.core.xbe.XBE;

import java.nio.file.Path;


public class GameName {
    private static void addRegionCode(XBE xbe, OGXArchive.Game game) {
        String regionCode = " (%s)".formatted(xbe.cert.getRegionCode());

        game.title += regionCode;
        game.iso_name += regionCode;
        game.xbe_title += regionCode;
    }

    public static OGXArchive.Game getForGame(XBE xbe, Path entry, boolean isArchiveEnabled) {
        if (!isArchiveEnabled) return OGXArchive.Game.buildDummy(xbe, entry);

        OGXArchive.Game archiveGame = OGXArchive.Game.retrieve(xbe.cert.dwTitleId);

        if (archiveGame == null) return OGXArchive.Game.buildDummy(xbe, entry);

        addRegionCode(xbe, archiveGame);

        return archiveGame;
    }
}
