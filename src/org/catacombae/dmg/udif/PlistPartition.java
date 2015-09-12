/*-
 * Copyright (C) 2007 Erik Larsson
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

package org.catacombae.dmg.udif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

public class PlistPartition {
    private String name;
    private String id;
    private String attributes;
    private UDIFBlock[] blockList;
    private long partitionSize;
    
    // Incoming variables
    private final long previousOutOffset;
    private final long previousInOffset;
    
    // Outgoing variables
    private long finalOutOffset = -1;
    private long finalInOffset = -1;
    
    public PlistPartition(String name, String id, String attributes, byte[] data,
            long previousOutOffset, long previousInOffset) throws IOException {
        this(name, id, attributes, new ByteArrayInputStream(data),
                previousOutOffset, previousInOffset);
    }

    public PlistPartition(String name, String id, String attributes, InputStream data,
            long previousOutOffset, long previousInOffset) throws IOException {
        this.name = name;
        this.id = id;
        this.attributes = attributes;
        this.previousOutOffset = previousOutOffset;
        this.previousInOffset = previousInOffset;

        this.blockList = parseBlocks(data);
        this.partitionSize = calculatePartitionSize(blockList);
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return id;
    }

    public String getAttributes() {
        return attributes;
    }

    public long getPartitionSize() {
        return partitionSize;
    }
    
    /** Copies all blocks to a newly allocated array. Might waste some memory. */
    public UDIFBlock[] getBlocks() {
        UDIFBlock[] res = new UDIFBlock[blockList.length];
        for(int i = 0; i < res.length; ++i)
            res[i] = blockList[i];
        return res;
    }

    /** Returns an iterator over all the UDIFBlocks that describe the contents of this partition. */
    public Iterator<UDIFBlock> getBlockIterator() {
        return new BlockIterator(blockList);
    }

    public int getBlockCount() {
        return blockList.length;
    }

    public long getFinalOutOffset() {
        if(finalOutOffset < 0)
            throw new RuntimeException("parseBlocks has not yet been called!");
        return finalOutOffset;
    }

    public long getFinalInOffset() {
        if(finalInOffset < 0)
            throw new RuntimeException("parseBlocks has not yet been called!");
        return finalInOffset;
    }
    
    private UDIFBlock[] parseBlocks(InputStream is) throws IOException {
        long bytesSkipped = is.read(new byte[0xCC]);

        if(bytesSkipped != 0xCC)
            throw new RuntimeException("Could not skip the desired amount of bytes...");

        int blockNumber = 0; // Increments by one for each block we read (each iteration in the while loop below)

        /* These two variables are part of the "hack" described below. */
        long lastByteReadInBlock = -1;
        boolean addInOffset = false;

        byte[] blockData = new byte[UDIFBlock.structSize()];

        LinkedList<UDIFBlock> blocks = new LinkedList<UDIFBlock>();

        int bytesRead = is.read(blockData);
        while(bytesRead > 0) { //offset <= data.length-UDIFBlock) {
            //System.err.println("Looping (read " + bytesRead + " bytes)");
            if(bytesRead != blockData.length)
                throw new RuntimeException("Could not read the desired amount of bytes... (desired: " + blockData.length + " read: " + bytesRead + ")");

            long inOffset = UDIFBlock.peekInOffset(blockData, 0);
            long inSize = UDIFBlock.peekInSize(blockData, 0);

            // Set compensation to the end of the output data of the previous partition to get true offset in outfile.
            long outOffsetCompensation = previousOutOffset;

            // Update pointer to the last byte read in the last block
            if(lastByteReadInBlock == -1)
                lastByteReadInBlock = inOffset;
            lastByteReadInBlock += inSize;

            /*
             * The lines below are a "hack" that I had to do to make dmgx work with
             * certain dmg-files. I don't understand the issue at all, which is why
             * this hack is here, but sometimes inOffset == 0 means that it is 0
             * relative to the previous partition's last inOffset. And sometimes it
             * doesn't (meaning the actual position 0 in the dmg file).
             */
            if(inOffset == 0 && blockNumber == 0) {
                Debug.notification("Detected inOffset == 0, setting addInOffset flag.");
                addInOffset = true;
            }
            long inOffsetCompensation = 0;
            if(addInOffset) {
                Debug.notification("addInOffset mode: inOffset tranformation " + inOffset + "->" +
                        (inOffset + previousInOffset));
                inOffsetCompensation = previousInOffset;
            }

            UDIFBlock currentBlock = new UDIFBlock(blockData, 0, outOffsetCompensation, inOffsetCompensation);
            blocks.add(currentBlock);
            ++blockNumber;

            //System.out.println("  " + currentBlock.toString());

            // Return if we have reached the end, and update
            if(currentBlock.getBlockType() == UDIFBlock.BT_END) {
                finalOutOffset = currentBlock.getTrueOutOffset();
                finalInOffset = previousInOffset + lastByteReadInBlock;

                if(is.read() != -1)
                    Debug.warning("Encountered additional data in blkx blob.");
                return blocks.toArray(new UDIFBlock[blocks.size()]);
            }

            bytesRead = is.read(blockData);
        }
        
        throw new RuntimeException("No BT_END block found!");
    }
        
    public static long calculatePartitionSize(UDIFBlock[] data) throws IOException {
        long partitionSize = 0;

        for(UDIFBlock db : data)
            partitionSize += db.getOutSize();
            
        return partitionSize;
    }
    
    private class BlockIterator implements Iterator<UDIFBlock> {

        private UDIFBlock[] blocks;
        private int pointer, endOffset;

        public BlockIterator(UDIFBlock[] blocks) {
            this(blocks, 0, blocks.length);
        }

        public BlockIterator(UDIFBlock[] blocks, int offset, int length) {
            this.blocks = blocks;
            this.pointer = offset;
            this.endOffset = offset + length;
        }

        public boolean hasNext() {
            return pointer < endOffset;
        }

        public UDIFBlock next() {
            return blocks[pointer++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
