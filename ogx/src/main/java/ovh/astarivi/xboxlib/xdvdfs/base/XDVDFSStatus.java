package ovh.astarivi.xboxlib.xdvdfs.base;

import org.tinylog.Logger;


public enum XDVDFSStatus {
    EXTRACTING_FILES(0),
    FINISHED(1),
    ERROR(2);

    private final int value;

    XDVDFSStatus(int val){
        value = val;
    }

    public int getValue() {
        return value;
    }

    public static XDVDFSStatus getFromValue(int val) {
        for (XDVDFSStatus e : XDVDFSStatus.values()) {
            if (e.getValue() == val) {
                return e;
            }
        }

        // We don't throw here as not to stop the process from Rust, it'll be interesting
        // to see what logs it produces in this case.
        Logger.error("Rust callback invalid XDVDFSStatus of {}", val);
        return EXTRACTING_FILES;
    }
}
