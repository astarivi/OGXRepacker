package ovh.astarivi.xboxlib.core.xbe;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;


public class XBE {
    public final XBEData.XBEHeader header;
    public final XBEData.XBECert cert;
    public byte[] rawCert = new byte[464];

    public XBE(String xbePath) throws IOException {
        try (RandomAccessFile bbis = new RandomAccessFile(xbePath, "r")) {
            byte[] headerData = new byte[376];
            bbis.read(headerData);
            header = new XBEData.XBEHeader(headerData);
            int certAddress = header.dwCertificateAddr - header.dwBaseAddr;

            bbis.seek(certAddress);
            bbis.read(rawCert);
            cert = new XBEData.XBECert(rawCert);
        } catch (IOException e) {
            Logger.error("Error reading XBE: {}", xbePath);
            Logger.error(e);
            throw e;
        }
    }
}