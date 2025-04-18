package ovh.astarivi.xboxlib.gui.utils;

import lombok.Getter;
import org.tinylog.Logger;

import java.nio.file.Path;


public record GuiConfig(
        Path inputField,
        Path outputField,
        Naming naming,
        Attacher attacher,
        Pack pack,
        Split split
) {
    public GuiConfig(Path inputField, Path outputField, int naming, int attacher, int pack, int split) {
        this(
                inputField,
                outputField,
                Naming.getFromValue(naming),
                Attacher.getFromValue(attacher),
                Pack.getFromValue(pack),
                Split.getFromValue(split)
        );
    }
    @Getter
    public enum Naming {
        OGXREPACKER(0),
        REPACKINATOR(1),
        KEEP_FILENAME(2);
        private final int value;

        Naming(int val) {
            value = val;
        }

        public static Naming getFromValue(int val) {
            for (Naming e : Naming.values()) {
                if (e.getValue() == val) {
                    return e;
                }
            }

            // We don't throw here as not to stop the process from Rust, it'll be interesting
            // to see what logs it produces in this case.
            Logger.error("Invalid value {} at {}", val, Naming.class);
            throw new RuntimeException();
        }
    }

    @Getter
    public enum Attacher {
        CERBIOS(0),
        STELLAR(1),
        DRIVE_IMAGE_UTILS(2),
        NONE(3);
        private final int value;

        Attacher(int val) {
            value = val;
        }

        public static Attacher getFromValue(int val) {
            for (Attacher e : Attacher.values()) {
                if (e.getValue() == val) {
                    return e;
                }
            }

            // We don't throw here as not to stop the process from Rust, it'll be interesting
            // to see what logs it produces in this case.
            Logger.error("Invalid value {} at {}", val, Attacher.class);
            throw new RuntimeException();
        }
    }

    @Getter
    public enum Pack {
        XDVDFS_AUTO(0),
        XDVDFS_REBUILD(1),
        XDVDFS_TRIM(2),
        XDVDFS_KEEP(3),
        CISO_AUTO(4),
        CISO_REBUILD(5),
        CISO_KEEP(6),
        EXTRACT(7);
        private final int value;

        Pack(int val) {
            value = val;
        }

        public static Pack getFromValue(int val) {
            for (Pack e : Pack.values()) {
                if (e.getValue() == val) {
                    return e;
                }
            }

            // We don't throw here as not to stop the process from Rust, it'll be interesting
            // to see what logs it produces in this case.
            Logger.error("Invalid value {} at {}", val, Pack.class);
            throw new RuntimeException();
        }
    }

    @Getter
    public enum Split {
        FATX(0),
        HALF(1),
        NO(2);
        private final int value;

        Split(int val) {
            value = val;
        }

        public static Split getFromValue(int val) {
            for (Split e : Split.values()) {
                if (e.getValue() == val) {
                    return e;
                }
            }

            // We don't throw here as not to stop the process from Rust, it'll be interesting
            // to see what logs it produces in this case.
            Logger.error("Invalid value {} at {}", val, Split.class);
            throw new RuntimeException();
        }
    }
}
