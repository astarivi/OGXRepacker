package ovh.astarivi.xboxlib.core.naming;

import ovh.astarivi.xboxlib.core.storage.OGXArchive;
import ovh.astarivi.xboxlib.core.xbe.XBE;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import java.nio.file.Path;


public class GameName {
    private static void addRegionCode(XBE xbe, OGXArchive.Game game, GuiConfig.Naming naming) {
        String regionCode = " (%s)".formatted(
                naming == GuiConfig.Naming.OGXREPACKER ? xbe.cert.getRegionCode() : xbe.cert.getRepackinatorRegionCode()
        );

        game.title += regionCode;
        game.iso_name += regionCode;
        game.xbe_title += regionCode;
    }

    public static OGXArchive.Game getForGame(XBE xbe, Path entry, GuiConfig.Naming naming) {
        if (naming == GuiConfig.Naming.KEEP_FILENAME) return OGXArchive.Game.buildDummy(xbe, entry);

        OGXArchive.Game archiveGame = OGXArchive.Game.retrieve(xbe.cert.dwTitleId);

        if (archiveGame == null) return OGXArchive.Game.buildDummy(xbe, entry);

        addRegionCode(xbe, archiveGame, naming);

        return archiveGame;
    }
}
