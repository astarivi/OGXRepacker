package ovh.astarivi.xboxlib.xdvdfs.layout;

import org.jetbrains.annotations.Nullable;

import ovh.astarivi.xboxlib.xdvdfs.XDVDFSImage;
import ovh.astarivi.xboxlib.xdvdfs.utils.StringDENPair;
import ovh.astarivi.xboxlib.xdvdfs.utils.StringDETPair;

import org.tinylog.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;


public class DirectoryEntry {
    public static class Table implements Cloneable {
        public Disk.Region region;

        public Table(ByteBuffer buffer) {
            region = new Disk.Region(buffer);
        }

        public Table(Disk.Region region) {
            this.region = region;
        }

        public boolean isEmpty() {
            return region.isEmpty();
        }

        public long offset(long offset) {
            return region.offset(offset);
        }

        public static EntryNode readTable(XDVDFSImage image, long offset) throws IOException {
            byte[] buffer = new byte[DiskNode.getSize()];

            image.seek(offset);
            image.read(buffer);

            byte[] emptyArray = new byte[DiskNode.getSize()];
            Arrays.fill(emptyArray, (byte) 0xff);


            if (Arrays.equals(buffer, new byte[DiskNode.getSize()]) || Arrays.equals(buffer, emptyArray)) {
                // It's empty
                return null;
            }

            EntryNode entryNode = new EntryNode(
                    new DiskNode(ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)),
                    new byte[256],
                    offset
            );

            image.seek(offset + 14);
            image.read(entryNode.name, 0, entryNode.node.dirent.filename_length);

            return entryNode;
        }

        public EntryNode findEntryNode(XDVDFSImage image, String name) throws IOException {
            if (isEmpty()) {
                throw new IllegalStateException(
                        "Trying to fetch DirectoryEntry.EntryNode for empty dir %s".formatted(name)
                );
            }

            long currentOffset = this.offset(0);

            while (true) {
                EntryNode dirent = readTable(image, currentOffset);

                if(dirent == null) {
                    throw new IllegalStateException("EntryNode is empty");
                }

                String direntName = dirent.getName();

                int cmp = name.compareToIgnoreCase(direntName);

                int nextOffset;

                if (cmp == 0) {
                    return dirent;
                } else if (cmp < 0) {
                    nextOffset = dirent.node.left_entry_offset;
                } else {
                    nextOffset = dirent.node.right_entry_offset;
                }

                if (nextOffset == 0) {
                    throw new IllegalArgumentException("Next EntryNode offset doesn't exist");
                }

                currentOffset = this.offset(4L * nextOffset);
            }
        }

        public EntryNode walkPath(XDVDFSImage image, String path) throws IOException {
            if(path.isEmpty() || path.equals("/")) {
                throw new IllegalArgumentException("Path is empty, or was root. No EntryNode here.");
            }

            Table directoryEntryTable = clone();

            List<String> pathSegments = Arrays.stream(path.trim().split("/"))
                    .filter(segment -> !segment.isEmpty())
                    .toList();

            ListIterator<String> iterator = pathSegments.listIterator();

            while(iterator.hasNext()) {
                String segment = iterator.next();

                EntryNode entryNode = directoryEntryTable.findEntryNode(image, segment);

                // Last item
                if (!iterator.hasNext()) {
                    return entryNode;
                }

                DiskData diskData = entryNode.node.dirent;

                directoryEntryTable = diskData.getTable();

                if (directoryEntryTable == null) {
                    throw new IllegalStateException("DiskData pointed to an empty Table sector");
                }
            }

            throw new IllegalStateException("Unreachable, all paths have been iterated and last entry didn't return");
        }

        public ArrayList<EntryNode> walkTree(XDVDFSImage image) throws IOException {
            ArrayList<EntryNode> dirents = new ArrayList<>();

            if (isEmpty()) return dirents;

            ArrayList<Long> stack = new ArrayList<>();
            stack.add(0L);

            int stack_length = stack.size();
            for (int i = 0; i < stack_length; i++) {
                long top = stack.get(i);
                long offset = this.offset(top);
                EntryNode dirent = readTable(image, offset);

                if (dirent == null) continue;
                Logger.info(
                        "[walkTree for sector {}] Found EntryNode {} at offset {}",
                        this.region.sector,
                        dirent.getName(),
                        top
                );

                int left_child = dirent.node.left_entry_offset;
                if (left_child != 0 && left_child != 0xffff) {
                    stack.add(4L * left_child);
                    stack_length += 1;
                }

                int right_child = dirent.node.right_entry_offset;
                if (right_child != 0 && right_child != 0xffff) {
                    stack.add(4L * right_child);
                    stack_length += 1;
                }

                dirents.add(dirent);
            }

            return dirents;
        }

        public ArrayList<StringDENPair> getFileTree(XDVDFSImage image) throws IOException {
            ArrayList<StringDENPair> dirents = new ArrayList<>();

            ArrayList<StringDETPair> stack = new ArrayList<>();
            stack.add(new StringDETPair(
                    "", this.clone()
            ));


            int stack_length = stack.size();
            for (int i = 0; i < stack_length; i++) {
                StringDETPair item = stack.get(i);

                ArrayList<EntryNode> children = item.tree().walkTree(image);
                for (EntryNode child : children) {
                    Table direntTable = child.node.dirent.getTable();

                    if (direntTable != null) {
                        String childName = child.getName();
                        stack.add(
                                new StringDETPair(
                                    "%s/%s".formatted(item.parent(), childName),
                                    direntTable
                                )
                        );
                        stack_length += 1;
                    }

                    dirents.add(
                            new StringDENPair(
                                    item.parent(),
                                    child.clone()
                            )
                    );
                }
            }

            return dirents;
        }

        @Override
        public Table clone() {
            try {
                Table clone = (Table) super.clone();
                clone.region = this.region.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class Attributes implements Cloneable {
        // 0
        public boolean read_only = false;
        // 1
        public boolean hidden = false;
        // 2
        public boolean system = false;
        // 4
        public boolean directory = false;
        // 5
        public boolean archive = false;
        // 7
        public boolean normal = false;

        public Attributes(ByteBuffer buffer) {
            byte flagsByte = buffer.get();

            read_only = (flagsByte & 0x01) != 0;
            hidden = (flagsByte & 0x02) != 0;
            system = (flagsByte & 0x04) != 0;
            directory = (flagsByte & 0x10) != 0;
            archive = (flagsByte & 0x20) != 0;
            normal = (flagsByte & 0x80) != 0;
        }

        @Override
        public Attributes clone() {
            try {
                return (Attributes) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class DiskData implements Cloneable{
        // 8 bytes
        public Disk.Region data;
        // u8 (1 byte)
        public Attributes attributes;
        // u8 (1 byte)
        public int filename_length;

        public DiskData(ByteBuffer buffer) {
            data = new Disk.Region(buffer);
            attributes = new Attributes(buffer);
            filename_length = Byte.toUnsignedInt(buffer.get());
        }

        public boolean isEmpty() {
            return data.isEmpty();
        }

        public boolean isDirectory() {
            return attributes.directory;
        }

        @Nullable
        public Table getTable() {
            if (isDirectory()) {
                return new Table(data);
            }

            return null;
        }

        public long getOffset(XDVDFSImage image) {
            return image.getRootOffset() + data.offset(0);
        }

        @Override
        public DiskData clone() {
            try {
                DiskData clone = (DiskData) super.clone();
                clone.data = data.clone();
                clone.attributes = attributes.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class DiskNode implements Cloneable{
        // u16 (2 bytes)
        public int left_entry_offset;
        // u16 (2 bytes)
        public int right_entry_offset;
        // 10 bytes
        public DiskData dirent;

        public DiskNode(ByteBuffer buffer) {
            left_entry_offset = Short.toUnsignedInt(buffer.getShort());
            right_entry_offset = Short.toUnsignedInt(buffer.getShort());
            dirent = new DiskData(buffer);
        }

        public static int getSize() {
            return 14;
        }

        @Override
        public DiskNode clone() {
            try {
                DiskNode clone = (DiskNode) super.clone();
                clone.dirent = dirent.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class EntryNode implements Cloneable {
        // 14 bytes
        public DiskNode node;
        // u8[256] (256 bytes)
        public byte[] name = new byte[256];
        // u64 (8 bytes)
        public long offset;

        public EntryNode(ByteBuffer buffer) {
            node = new DiskNode(buffer);
            buffer.get(name);
            offset = buffer.getLong();
        }

        public EntryNode(DiskNode diskNode, byte[] name, long offset) {
            this.node = diskNode;
            this.name = name;
            this.offset = offset;
        }

        public byte[] getNameSlice(){
            return Arrays.copyOfRange(name, 0, node.dirent.filename_length);
        }

        public String getName() {
            byte[] nameBytes = getNameSlice();
            return new String(nameBytes, StandardCharsets.ISO_8859_1);
        }

        public static int getSize() {
            return 278;
        }

        @Override
        public EntryNode clone() {
            try {
                EntryNode clone = (EntryNode) super.clone();
                clone.node = node.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }
}
