package ovh.astarivi.xboxlib.core.xbe;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;


public class XBE {
    public final XBEData.XBEHeader header;
    public final XBEData.XBECert cert;
    public byte[] rawCert = new byte[464];

    public XBE(RandomAccessFile bbis) throws IOException {
        try {
            byte[] headerData = new byte[376];
            bbis.read(headerData);
            header = new XBEData.XBEHeader(headerData);
            int certAddress = header.dwCertificateAddr - header.dwBaseAddr;

            bbis.seek(bbis.getFilePointer() + certAddress);
            bbis.read(rawCert);
            cert = new XBEData.XBECert(rawCert);
        } catch (IOException e) {
            Logger.error("Error parsing XBE from file with offset {}", bbis.getFilePointer());
            Logger.error(e);
            throw e;
        }
    }
}