package ovh.astarivi.xboxlib.core.xbe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


public class XBEData {
    public static class XBEHeader {
        public static final int XOR_EP_DEBUG = 0x94859D4B;  // Entry Point (Debug)
        public static final int XOR_EP_RETAIL = 0xA8FC57AB; // Entry Point (Retail)
        public static final int XOR_KT_DEBUG = 0xEFB1F152;  // Kernel Thunk (Debug)
        public static final int XOR_KT_RETAIL = 0x5B6D40B6; // Kernel Thunk (Retail)

        public String dwMagic; // Magic number [should be "XBEH"]
        public byte[] pbDigitalSignature; // Digital signature
        public int dwBaseAddr; // Base address
        public int dwSizeofHeaders; // Size of headers
        public int dwSizeofImage; // Size of image
        public int dwSizeofImageHeader; // Size of image header
        public int dwTimeDate; // Timedate stamp
        public int dwCertificateAddr; // Certificate address
        public int dwSections; // Number of sections
        public int dwSectionHeadersAddr; // Section headers address

        // Struct init_flags
        public int dwInitFlags; // Mount utility drive flag
        public boolean init_flags_mount_utility_drive; // Mount utility drive flag
        public boolean init_flags_format_utility_drive; // Format utility drive flag
        public boolean init_flags_limit_64mb; // Limit development kit run time memory to 64mb flag
        public boolean init_flags_dont_setup_harddisk; // Don't setup hard disk flag
        public boolean init_flags_unused; // Unused (or unknown)
        public boolean init_flags_unused_b1; // Unused (or unknown)
        public boolean init_flags_unused_b2; // Unused (or unknown)
        public boolean init_flags_unused_b3; // Unused (or unknown)

        public int dwEntryAddr; // Entry point address
        public int dwEntryAddr_f; // Entry point address
        public int dwTLSAddr; // TLS directory address
        public int dwPeStackCommit; // Size of stack commit
        public int dwPeHeapReserve; // Size of heap reserve
        public int dwPeHeapCommit; // Size of heap commit
        public int dwPeBaseAddr; // Original base address
        public int dwPeSizeofImage; // Size of original image
        public int dwPeChecksum; // Original checksum
        public int dwPeTimeDate; // Original timedate stamp
        public int dwDebugPathnameAddr; // Debug pathname address
        public int dwDebugFilenameAddr; // Debug filename address
        public int dwDebugUnicodeFilenameAddr; // Debug unicode filename address
        public int dwKernelImageThunkAddr; // Kernel image thunk address
        public int dwNonKernelImportDirAddr; // Non kernel import directory address
        public int dwLibraryVersions; // Number of library versions
        public int dwLibraryVersionsAddr; // Library versions address
        public int dwKernelLibraryVersionAddr; // Kernel library version address
        public int dwXAPILibraryVersionAddr; // XAPI library version address
        public int dwLogoBitmapAddr; // Logo bitmap address
        public int dwSizeofLogoBitmap; // Logo bitmap size

        public XBEHeader(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            dwMagic = new String(buffer.array(), 0, 4);
            pbDigitalSignature = Arrays.copyOfRange(buffer.array(), 4, 260);
            buffer.position(260);
            dwBaseAddr = buffer.getInt();
            dwSizeofHeaders = buffer.getInt();
            dwSizeofImage = buffer.getInt();
            dwSizeofImageHeader = buffer.getInt();
            dwTimeDate = buffer.getInt();
            dwCertificateAddr = buffer.getInt();
            dwSections = buffer.getInt();
            dwSectionHeadersAddr = buffer.getInt();

            dwInitFlags = buffer.getInt();
            init_flags_mount_utility_drive = (dwInitFlags & 0x1) != 0;
            init_flags_format_utility_drive = (dwInitFlags & 0x2) != 0;
            init_flags_limit_64mb = (dwInitFlags & 0x4) != 0;
            init_flags_dont_setup_harddisk = (dwInitFlags & 0x8) != 0;
            init_flags_unused = (dwInitFlags & 0x10) != 0;
            init_flags_unused_b1 = (dwInitFlags & 0x20) != 0;
            init_flags_unused_b2 = (dwInitFlags & 0x40) != 0;
            init_flags_unused_b3 = (dwInitFlags & 0x80) != 0;

            dwEntryAddr = buffer.getInt();
            dwTLSAddr = buffer.getInt();
            dwPeStackCommit = buffer.getInt();
            dwPeHeapReserve = buffer.getInt();
            dwPeHeapCommit = buffer.getInt();
            dwPeBaseAddr = buffer.getInt();
            dwPeSizeofImage = buffer.getInt();
            dwPeChecksum = buffer.getInt();
            dwPeTimeDate = buffer.getInt();
            dwDebugPathnameAddr = buffer.getInt();
            dwDebugFilenameAddr = buffer.getInt();
            dwDebugUnicodeFilenameAddr = buffer.getInt();
            dwKernelImageThunkAddr = buffer.getInt();
            dwNonKernelImportDirAddr = buffer.getInt();
            dwLibraryVersions = buffer.getInt();
            dwLibraryVersionsAddr = buffer.getInt();
            dwKernelLibraryVersionAddr = buffer.getInt();
            dwXAPILibraryVersionAddr = buffer.getInt();
            dwLogoBitmapAddr = buffer.getInt();
            dwSizeofLogoBitmap = buffer.getInt();

            dwEntryAddr_f = dwEntryAddr ^ XOR_EP_RETAIL;
        }
    }

    public static class XBECert {
        public int dwSize; // 0x0000 - size of certificate
        public int dwTimeDate; // 0x0004 - timedate stamp
        public String dwTitleId; // Title ID
        public byte[] wszTitleName; // 0x000C - title name (unicode)
        public byte[] dwAlternateTitleId; // 0x005C - alternate title ids
        public int dwAllowedMedia; // 0x009C - allowed media types
        public int dwGameRegion; // 0x00A0 - game region
        public boolean regionUSA;
        public boolean regionJapan;
        public boolean regionEurope;
        public boolean regionManufacturing;
        public int dwGameRatings; // 0x00A4 - game ratings
        public int dwDiskNumber; // 0x00A8 - disk number
        public int dwVersion; // 0x00AC - version
        public byte[] bzLanKey; // 0x00B0 - lan key
        public byte[] bzSignatureKey; // 0x00C0 - signature key
        public byte[][] bzTitleAlternateSignatureKey; // 0x00D0 - alternate signature keys
        public String cleanTitleName; // Title name cleaned up

        public XBECert(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            dwSize = buffer.getInt();
            dwTimeDate = buffer.getInt();
            int intermediateTitleId = buffer.getInt();
            dwTitleId = String.format("%08X", intermediateTitleId).toUpperCase();
            wszTitleName = new byte[80];
            buffer.get(wszTitleName);
            dwAlternateTitleId = new byte[16];
            buffer.get(dwAlternateTitleId);
            dwAllowedMedia = buffer.getInt();
            dwGameRegion = buffer.getInt();
            regionUSA = (dwGameRegion & 0x00000001) != 0;
            regionJapan = (dwGameRegion & 0x00000002) != 0;
            regionEurope = (dwGameRegion & 0x00000004) != 0;
            regionManufacturing = (dwGameRegion & 0x80000000) != 0;
            dwGameRatings = buffer.getInt();
            dwDiskNumber = buffer.getInt();
            dwVersion = buffer.getInt();
            bzLanKey = new byte[16];
            buffer.get(bzLanKey);
            bzSignatureKey = new byte[16];
            buffer.get(bzSignatureKey);
            bzTitleAlternateSignatureKey = new byte[16][16];
            for (int i = 0; i < 16; i++) {
                buffer.get(bzTitleAlternateSignatureKey[i]);
            }

            cleanTitleName = new String(wszTitleName, java.nio.charset.StandardCharsets.UTF_16LE).trim();
        }

        public String getRegionCode() {
            // World
            if(regionUSA && regionJapan && regionEurope) {
                return "W";
            }

            if(!regionUSA && !regionJapan && !regionEurope) {
                return "Unk";
            }

            String code = "";

            if(regionUSA) {
                code += "U";
            }

            if(regionJapan) {
                code += "J";
            }

            if(regionEurope) {
                code += "E";
            }

            return code;
        }
    }

    public static class XBESection {
        public String name;
        public byte[] data;
        public boolean flag_writable;
        public boolean flag_preload;
        public boolean flag_executable;
        public boolean flag_inserted_file;
        public boolean flag_head_page_ro;
        public boolean flag_tail_page_ro;
        public boolean flag_unused_a1;
        public boolean flag_unused_a2;
        public int dwVirtualAddr; // Virtual address
        public int dwVirtualSize; // Virtual size
        public int dwRawAddr; // File offset to raw data
        public int dwSizeofRaw; // Size of raw data
        public int dwSectionNameAddr; // Section name addr
        public int dwSectionRefCount; // Section reference count
        public int dwHeadSharedRefCountAddr; // Head shared page reference count address
        public int dwTailSharedRefCountAddr; // Tail shared page reference count address
        public byte[] bzSectionDigest; // Section digest

        public XBESection(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            byte flagsByte = buffer.get();
            flag_writable = (flagsByte & 0x1) != 0;
            flag_preload = (flagsByte & 0x2) != 0;
            flag_executable = (flagsByte & 0x4) != 0;
            flag_inserted_file = (flagsByte & 0x8) != 0;
            flag_head_page_ro = (flagsByte & 0x10) != 0;
            flag_tail_page_ro = (flagsByte & 0x20) != 0;
            flag_unused_a1 = (flagsByte & 0x40) != 0;
            flag_unused_a2 = (flagsByte & 0x80) != 0;

            dwVirtualAddr = buffer.getInt();
            dwVirtualSize = buffer.getInt();
            dwRawAddr = buffer.getInt();
            dwSizeofRaw = buffer.getInt();
            dwSectionNameAddr = buffer.getInt();
            dwSectionRefCount = buffer.getInt();
            dwHeadSharedRefCountAddr = buffer.getInt();
            dwTailSharedRefCountAddr = buffer.getInt();
            bzSectionDigest = new byte[20];
            buffer.get(bzSectionDigest);
        }
    }
}
