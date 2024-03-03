package ovh.astarivi.xboxlib.xdvdfs.utils;

import ovh.astarivi.xboxlib.xdvdfs.layout.DirectoryEntry;

public record StringDENPair(String parent, DirectoryEntry.EntryNode node) {
    @Override
    public String toString() {
        return "StringDENPair{" +
                "parent='" + parent + '\'' +
                ", node=" + node +
                '}';
    }
}
