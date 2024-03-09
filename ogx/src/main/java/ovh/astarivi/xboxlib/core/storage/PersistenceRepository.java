package ovh.astarivi.xboxlib.core.storage;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.Map;

@Getter
public class PersistenceRepository {
    @Getter(AccessLevel.NONE)
    private static volatile PersistenceRepository _instance = null;
    private final AppProperties appProperties;
    private final Map<String, OGXArchive.Game> gamesArchive;

    public PersistenceRepository() {
        appProperties = new AppProperties("ogxrepacker.properties");
        try {
            gamesArchive = OGXArchive.loadArchive();
        } catch (IOException e) {
            Logger.error("Failure to load archive.json");
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public static @NotNull PersistenceRepository getInstance() {
        if (_instance == null) {
            synchronized (PersistenceRepository.class) {
                if (_instance == null) _instance = new PersistenceRepository();
            }
        }
        return _instance;
    }


}
