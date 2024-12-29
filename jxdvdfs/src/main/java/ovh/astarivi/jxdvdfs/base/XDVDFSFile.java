package ovh.astarivi.jxdvdfs.base;

/**
 * Contains information about a file inside a XDVDFS volume.
 * <p>
 * All sizes are in bytes.
 *
 * @param offset the offset to the file, from the start of the image; all other offsets included
 * @param size the reported size of this file
 */
public record XDVDFSFile(long offset, long size) {
}
