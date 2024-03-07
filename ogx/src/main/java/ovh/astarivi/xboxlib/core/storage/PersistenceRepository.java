package ovh.astarivi.xboxlib.core.storage;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PersistenceRepository {
    @Getter(AccessLevel.NONE)
    private static volatile PersistenceRepository _instance = null;
    private final AppProperties appProperties;

    public PersistenceRepository() {
        appProperties = new AppProperties("ogxrepacker.properties");
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
