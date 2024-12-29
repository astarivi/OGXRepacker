package ovh.astarivi.jxdvdfs.base;

/**
 * Contains information about the data contained in the XDVDFS volume, and of the volume itself.
 * <p>
 * All sizes are in bytes.
 *
 * @param fileCount the file count inside the volume
 * @param totalSize the sum of all the file sizes inside the volume
 * @param startOffset the offset at which the XDVDFS volume is found in the image
 * @param maxSize the max size taken by the image, accounting for all types of padding
 */
public record XDVDFSStat(long fileCount, long totalSize, long startOffset, long maxSize) {
    @Override
    public String toString() {
        return "XDVDFSStat{" +
                "fileCount=" + fileCount +
                ", totalSize=" + totalSize +
                ", startOffset=" + startOffset +
                ", maxSize=" + maxSize +
                '}';
    }
}
