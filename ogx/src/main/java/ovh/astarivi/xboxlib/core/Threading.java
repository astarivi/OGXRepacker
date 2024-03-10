package ovh.astarivi.xboxlib.core;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Getter
public class Threading {
    @Getter(AccessLevel.NONE)
    private static volatile Threading _instance = null;
    private final ExecutorService processExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService instantExecutor = Executors.newCachedThreadPool();

    private Threading() {
        if (_instance != null) {
            throw new RuntimeException("Duplicated singleton Threading");
        }
    }

    public static @NotNull Threading getInstance() {
        if (_instance == null) {
            synchronized (Threading.class) {
                if (_instance == null) _instance = new Threading();
            }
        }
        return _instance;
    }

    @Override
    protected void finalize() throws Throwable {
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            processExecutor.shutdown();
            instantExecutor.shutdownNow();
            processExecutor.awaitTermination(2, TimeUnit.SECONDS);
            instantExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (Throwable th) {
            throw th;
        } finally {
            super.finalize();
        }
    }
}
