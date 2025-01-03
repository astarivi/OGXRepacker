package ovh.astarivi.xboxlib.core.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.core.xbe.XBE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class OGXArchive {
    // FIXME: Some of these can actually be rebuilt just fine, investigate further
    // Source: https://consolemods.org/wiki/Xbox:Patching_XBEs
    public static List<String> TRIM_ONLY_TITLES = Arrays.asList(
            "41430018", // All Star Baseball 2004
            "5451000F", // Alter Echo,
            "4D530041", // Amped 2,
            "49470022", // Enter The Matrix
            "4947000B", // Furious Karting
            "4541004C", // Harry Potter and the Prisoner of Azkaban
            "434D0046", // LMA Manager 2005
            "434D0049", // Manchester United Manager 2005
            "54540079", // Midnight Club 3: DUB Edition Remix
            "41560041", // Monster Garage
            "48450001", // Rugby League
            "4A57000A", // Ski Racing 2006
            "434D0011", // ToCA Race Driver 2
            "434D0050", // ToCA Race Driver 3
            "5553005F", // Tom Clancy's Rainbow Six : Critical Hour
            "41560049", // Tony Hawk's American Wasteland
            "4156005E", // Tony Hawk's Project 8,
            "41560045", // True Crime: New York City
            "45410042", // 007: Everything or Nothing
            "56550039", // The Incredible Hulk: Ultimate Destruction
            "5655002F", // Fight Club
            "434D0024", // IndyCar Series 2005
            "4D53000B", // NBA Inside Drive 2004
            "56550026", // The Chronicles of Riddick: Escape from Butcher Bay
            "56550031" // Leisure Suit Larry: Magna Cum Laude
    );

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Game implements Cloneable{
        public String title_id;
        public String title;
        public String iso_name;
        public String xbe_title;

        @SuppressWarnings("unused")
        public Game() {
        }

        @JsonIgnore
        public Game(String title_id, String title, String iso_name, String xbe_title) {
            this.title_id = title_id;
            this.title = title;
            this.iso_name = iso_name;
            this.xbe_title = xbe_title;
        }

        @JsonIgnore
        public static @Nullable OGXArchive.Game retrieve(String titleId) {
            OGXArchive.Game g = PersistenceRepository.getInstance().getGamesArchive().get(
                    titleId
            );

            if (g == null) {
                return null;
            }

            return g.clone();
        }

        @JsonIgnore
        public static OGXArchive.Game buildDummy(XBE xbe, Path entry) {
            String entryFilename = Utils.toFatXFilename(
                    Utils.removeFileExtension(
                            entry.getFileName().toString(),
                            true
                    )
            );

            return new OGXArchive.Game(
                    xbe.cert.dwTitleId,
                    entryFilename.substring(0, Math.min(entryFilename.length(), 42)),
                    entryFilename.substring(0, Math.min(entryFilename.length(), 36)),
                    entryFilename.substring(0, Math.min(entryFilename.length(), 40))
            );
        }

        @JsonIgnore
        @Override
        public Game clone() {
            try {
                return (Game) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static Map<String, Game> loadArchive() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = OGXArchive.class.getClassLoader().getResourceAsStream(
                "ovh/astarivi/xboxlib/res/archive.json"
        )) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        }
    }
}
