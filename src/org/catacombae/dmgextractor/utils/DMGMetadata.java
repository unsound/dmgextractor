/*-
 * Copyright (C) 2006 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catacombae.dmgextractor.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

public class DMGMetadata {

    public static final long PLIST_ADDRESS_1 = 0x1E0;
    public static final long PLIST_ADDRESS_2 = 0x128;
    public final byte[] rawData;
    public final byte[] plistXmlData;
    public final byte[] unknown1_256;
    public PartitionBlockList[] blockLists;
    public final byte[] unknown2_12;
    public APMPartition[] partitions;
    public final byte[] unknown3_unknown;
    public final byte[] koly;

    public DMGMetadata(RandomAccessFile dmgFile) throws IOException {
        dmgFile.seek(dmgFile.length() - PLIST_ADDRESS_1);
        long plistBegin1 = dmgFile.readLong();
        long plistEnd = dmgFile.readLong();
        dmgFile.seek(dmgFile.length() - PLIST_ADDRESS_2);
        long plistBegin2 = dmgFile.readLong();
        long plistSize = dmgFile.readLong();

        rawData = new byte[(int) (dmgFile.length() - plistBegin1)];
        dmgFile.seek(plistBegin1);
        dmgFile.readFully(rawData);

        plistXmlData = new byte[(int) plistSize];
        dmgFile.seek(plistBegin1);
        dmgFile.readFully(plistXmlData);

        unknown1_256 = new byte[256];
        dmgFile.readFully(unknown1_256);

        LinkedList<PartitionBlockList> blockListList = new LinkedList<PartitionBlockList>();
        int length = dmgFile.readInt();
        byte[] fourcc = new byte[4];
        dmgFile.readFully(fourcc);
        String fourccString = new String(fourcc, "US-ASCII");
        dmgFile.seek(dmgFile.getFilePointer() - 4);
        while(fourccString.equals("mish")) {
            blockListList.add(new PartitionBlockList(dmgFile, length));
            length = dmgFile.readInt();
            dmgFile.readFully(fourcc);
            fourccString = new String(fourcc, "US-ASCII");
            dmgFile.seek(dmgFile.getFilePointer() - 4);
        }
        blockLists = blockListList.toArray(new PartitionBlockList[blockListList.size()]);

        unknown2_12 = new byte[12];
        dmgFile.readFully(unknown2_12);

        LinkedList<APMPartition> partitionList = new LinkedList<APMPartition>();
        byte[] currentPartitionEntry = new byte[0x200];
        dmgFile.readFully(currentPartitionEntry);
        byte[] pmSig = new byte[2];
        pmSig[0] = currentPartitionEntry[0];
        pmSig[1] = currentPartitionEntry[1];
        while(new String(pmSig, "US-ASCII").equals("PM")) {
            partitionList.addLast(new APMPartition(currentPartitionEntry));
            dmgFile.readFully(currentPartitionEntry);
            pmSig[0] = currentPartitionEntry[0];
            pmSig[1] = currentPartitionEntry[1];
        }
        while(onlyZeros(currentPartitionEntry))
            dmgFile.readFully(currentPartitionEntry);
        partitions = partitionList.toArray(new APMPartition[partitionList.size()]);

        unknown3_unknown = new byte[(int) (dmgFile.length() - dmgFile.getFilePointer() - 512)];
        dmgFile.readFully(unknown3_unknown);

        koly = new byte[512];
        dmgFile.seek(dmgFile.length() - koly.length);
        dmgFile.readFully(koly);

        if(dmgFile.getFilePointer() != dmgFile.length())
            System.out.println("MISCALCULATION! FP=" + dmgFile.getFilePointer() + " LENGTH=" + dmgFile.length());
    }

    public void printInfo(PrintStream ps) {
        ps.println("block list:");
        for(PartitionBlockList pbl : blockLists)
            pbl.printInfo(ps);
        ps.println("partitions:");
        for(APMPartition ap : partitions)
            ap.printPartitionInfo(ps);
    }

    private static boolean onlyZeros(byte[] array) {
        for(int i = 0; i < array.length; ++i) {
            if(array[i] != 0)
                return false;
        }
        return true;
    }

    public static class PartitionBlockList {

        public final byte[] header = new byte[0xCC];
        public final BlockDescriptor[] descriptors;

        public PartitionBlockList(byte[] entryData) throws IOException {
            this(new DataInputStream(new ByteArrayInputStream(entryData)), entryData.length);
        }

        public PartitionBlockList(DataInput di, int length) throws IOException {
            int position = 0;
            di.readFully(header);
            position += header.length;
            LinkedList<BlockDescriptor> descs = new LinkedList<BlockDescriptor>();
            while(position < length) {
                descs.addLast(new BlockDescriptor(di));
                position += 0x28;
            }
            descriptors = descs.toArray(new BlockDescriptor[descs.size()]);
        }

        public void printInfo(PrintStream ps) {
            for(BlockDescriptor bd : descriptors)
                ps.println(bd.toString());
        }
    }

    public static class BlockDescriptor {
        // Known block types

        public static final int BT_COPY = 0x00000001;
        public static final int BT_ZERO = 0x00000002;
        public static final int BT_ZLIB = 0x80000005;
        public static final int BT_END = 0xffffffff;
        public static final int BT_UNKNOWN1 = 0x7ffffffe;
        private static final int[] KNOWN_BLOCK_TYPES = { BT_COPY,
            BT_ZERO,
            BT_ZLIB,
            BT_END,
            BT_UNKNOWN1 };
        private static final String[] KNOWN_BLOCK_TYPE_NAMES = { "BT_COPY",
            "BT_ZERO",
            "BT_ZLIB",
            "BT_END",
            "BT_UNKNOWN1" };
        private int blockType;
        private int unknown;
        private long outOffset;
        private long outSize;
        private long inOffset;
        private long inSize;

        public BlockDescriptor() {
        }

        public BlockDescriptor(byte[] entryData) throws IOException {
            this(new DataInputStream(new ByteArrayInputStream(entryData)));
        }

        public BlockDescriptor(DataInput dataIn) throws IOException {
            blockType = dataIn.readInt();
            unknown = dataIn.readInt();
            outOffset = dataIn.readLong() * 0x200;
            outSize = dataIn.readLong() * 0x200;
            inOffset = dataIn.readLong();
            inSize = dataIn.readLong();
        }

        public byte[] toBytes() throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream(0x28);
            DataOutputStream dataOut = new DataOutputStream(result);

            dataOut.writeInt(blockType); // 4 bytes
            dataOut.writeInt(unknown); // 4 bytes
            if((outOffset % 0x200) != 0)
                throw new RuntimeException("Out offset must be aligned to 0x200 block size!");
            dataOut.writeLong(outOffset / 0x200); // 8 bytes
            if((outSize % 0x200) != 0)
                throw new RuntimeException("Out size must be aligned to 0x200 block size!");
            dataOut.writeLong(outSize / 0x200); // 8 bytes
            dataOut.writeLong(inOffset); // 8 bytes
            dataOut.writeLong(inSize); // 8 bytes
            // sum = 4 + 4 + 8 + 8 + 8 + 8 = 40 = 0x28

            dataOut.flush();
            dataOut.close();
            return result.toByteArray();
        }

        public int getBlockType() {
            return blockType;
        }

        public int getUnknown() {
            return unknown;
        }

        public String getUnknownAsString() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
            DataOutputStream dos = new DataOutputStream(baos);
            dos.write(unknown);
            dos.close();
            return new String(baos.toByteArray(), "US-ASCII");
        }

        public long getOutOffset() {
            return outOffset;
        }

        public long getOutSize() {
            return outSize;
        }

        public long getInOffset() {
            return inOffset;
        }

        public long getInSize() {
            return inSize;
        }

        public void setBlockType(int blockType) {
            this.blockType = blockType;
        }

        public void setUnknown(int unknown) {
            this.unknown = unknown;
        }

        public void setOutOffset(long outOffset) {
            if((outOffset % 0x200) != 0)
                throw new RuntimeException("Out offset must be aligned to 0x200 block size!");
            this.outOffset = outOffset;
        }

        public void setOutSize(long outSize) {
            if((outSize % 0x200) != 0)
                throw new RuntimeException("Out size must be aligned to 0x200 block size!");
            this.outSize = outSize;
        }

        public void setInOffset(long inOffset) {
            this.inOffset = inOffset;
        }

        public void setInSize(long inSize) {
            this.inSize = inSize;
        }

        public boolean hasKnownBlockType() {
            for(int current : KNOWN_BLOCK_TYPES) {
                if(blockType == current)
                    return true;
            }
            return false;
        }

        public String getBlockTypeName() {
            for(int i = 0; i < KNOWN_BLOCK_TYPES.length; ++i) {
                int current = KNOWN_BLOCK_TYPES[i];
                if(blockType == current)
                    return KNOWN_BLOCK_TYPE_NAMES[i];
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("[BlockDescriptor");

            String blockTypeString = "\"" + getBlockTypeName() + "\"";
            if(blockTypeString == null)
                blockTypeString = "0x" + Integer.toHexString(blockType) + " (unknown type)";

            result.append(" blockType=" + blockTypeString);
            result.append(" unknown=" + Integer.toHexString(unknown));
            result.append(" outOffset=" + outOffset);
            result.append(" outSize=" + outSize);
            result.append(" inOffset=" + inOffset);
            result.append(" inSize=" + inSize);
            result.append("]");

            return result.toString();
        }
    }

    // Saxat från HFSExplorer.java
    public static class APMPartition {

        public int pmSig; // {partition signature}
        public int pmSigPad; // {reserved}
        public long pmMapBlkCnt; // {number of blocks in partition map}
        public long pmPyPartStart; // {first physical block of partition}
        public long pmPartBlkCnt; // {number of blocks in partition}
        public final byte[] pmPartName = new byte[32]; // {partition name}
        public final byte[] pmParType = new byte[32]; // {partition type}
        public long pmLgDataStart; // {first logical block of data area}
        public long pmDataCnt; // {number of blocks in data area}
        public long pmPartStatus; // {partition status information}
        public long pmLgBootStart; // {first logical block of boot code}
        public long pmBootSize; // {size of boot code, in bytes}
        public long pmBootAddr; // {boot code load address}
        public long pmBootAddr2; // {reserved}
        public long pmBootEntry; // {boot code entry point}
        public long pmBootEntry2; // {reserved}
        public long pmBootCksum; // {boot code checksum}
        public final byte[] pmProcessor = new byte[16]; // {processor type}
        public final int[] pmPad = new int[188]; // {reserved}

        public APMPartition(byte[] entryData) throws IOException {
            this(new DataInputStream(new ByteArrayInputStream(entryData)));
        }

        public APMPartition(DataInput di) throws IOException {
            // 2*2 + 4*3 + 32*2 + 10*4 + 16 + 188*2 = 512
            pmSig = di.readShort() & 0xffff;
            pmSigPad = di.readShort() & 0xffff;
            pmMapBlkCnt = di.readInt() & 0xffffffffL;
            pmPyPartStart = di.readInt() & 0xffffffffL;
            pmPartBlkCnt = di.readInt() & 0xffffffffL;
            di.readFully(pmPartName);
            di.readFully(pmParType);
            pmLgDataStart = di.readInt() & 0xffffffffL;
            pmDataCnt = di.readInt() & 0xffffffffL;
            pmPartStatus = di.readInt() & 0xffffffffL;
            pmLgBootStart = di.readInt() & 0xffffffffL;
            pmBootSize = di.readInt() & 0xffffffffL;
            pmBootAddr = di.readInt() & 0xffffffffL;
            pmBootAddr2 = di.readInt() & 0xffffffffL;
            pmBootEntry = di.readInt() & 0xffffffffL;
            pmBootEntry2 = di.readInt() & 0xffffffffL;
            pmBootCksum = di.readInt() & 0xffffffffL;
            di.readFully(pmProcessor);
            for(int i = 0; i < pmPad.length; ++i)
                pmPad[i] = di.readShort() & 0xffff;
        }

        public void printPartitionInfo(PrintStream ps) {
// 	    String result = "";
// 	    result += "Partition name: \"" + new String(pmPartName) + "\"\n";
// 	    result += "Partition type: \"" + new String(pmParType) + "\"\n";
// 	    result += "Processor type: \"" + new String(pmProcessor) + "\"\n";
// 	    return result;
            try {
                ps.println("pmSig: " + pmSig);
                ps.println("pmSigPad: " + pmSigPad);
                ps.println("pmMapBlkCnt: " + pmMapBlkCnt);
                ps.println("pmPyPartStart: " + pmPyPartStart);
                ps.println("pmPartBlkCnt: " + pmPartBlkCnt);
                ps.println("pmPartName: \"" + new String(pmPartName, "US-ASCII") + "\"");
                ps.println("pmParType: \"" + new String(pmParType, "US-ASCII") + "\"");
                ps.println("pmLgDataStart: " + pmLgDataStart);
                ps.println("pmDataCnt: " + pmDataCnt);
                ps.println("pmPartStatus: " + pmPartStatus);
                ps.println("pmLgBootStart: " + pmLgBootStart);
                ps.println("pmBootSize: " + pmBootSize);
                ps.println("pmBootAddr: " + pmBootAddr);
                ps.println("pmBootAddr2: " + pmBootAddr2);
                ps.println("pmBootEntry: " + pmBootEntry);
                ps.println("pmBootEntry2: " + pmBootEntry2);
                ps.println("pmBootCksum: " + pmBootCksum);
                ps.println("pmProcessor: \"" + new String(pmProcessor, "US-ASCII") + "\"");
                ps.println("pmPad: " + pmPad);
            } catch(UnsupportedEncodingException uee) {
                uee.printStackTrace();
            } // Will never happen. Ever. Period.
        }
    }

    public static void main(String[] args) throws IOException {
        DMGMetadata meta = new DMGMetadata(new RandomAccessFile(args[0], "r"));
        meta.printInfo(System.out);
    }
}
