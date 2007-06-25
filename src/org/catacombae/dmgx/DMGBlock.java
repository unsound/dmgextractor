/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.catacombae.dmgx;

public class DMGBlock {
    /** This blocktype means the data is compressed using some "ADC" algorithm that I have no idea how to decompress... */
    public static final int BT_ADC = 0x80000004;
    
    /** This blocktype means the data is compressed with zlib. */
    public static final int BT_ZLIB = 0x80000005;
    
    /** This blocktype means the data is compressed with the bzip2 compression algorithm. These blocktypes are unsupported,
	as I haven't found a GPL-compatible bzip2 decompressor written in Java yet. */
    public static final int BT_BZIP2 = 0x80000006;
    
    /** This blocktype means the data is uncompressed and can simply be copied. */
    public static final int BT_COPY = 0x00000001;
    
    /** This blocktype represents a fill of zeroes. */
    public static final int BT_ZERO = 0x00000002;
    
    /** This blocktype represents a fill of zeroes (the difference between this blocktype and BT_ZERO is not documented, 
	and parsing of these blocks is experimental). */
    public static final int BT_ZERO2 = 0x00000000;
    
    /** This blocktype indicates the end of the partition. */
    public static final int BT_END = 0xffffffff;
    
    /** This blocktype has been observed, but its purpose is currently unknown. In all the observed cases the outSize was
	equal to 0, so it's probably some marker, like BT_END. */
    public static final int BT_UNKNOWN = 0x7ffffffe;
    
    /*
     * 4
     * 4
     * 8
     * 8
     * 8
     * 8
     * ---
     * 40 bytes / 0x28 bytes
     */
    private int blockType;
    private int skipped;
    private long outOffset;
    private long outSize;
    private long inOffset;
    private long inSize;
    
    private long outOffsetComp = 0;
    private long inOffsetComp = 0;
    
    public DMGBlock(byte[] data, int offset) {
	this.blockType = Util.readIntBE(data, offset+0);
	this.skipped = Util.readIntBE(data, offset+4);
	this.outOffset = Util.readLongBE(data, offset+8)*0x200;
	this.outSize = Util.readLongBE(data, offset+16)*0x200;
	this.inOffset = Util.readLongBE(data, offset+24);
	this.inSize = Util.readLongBE(data, offset+32);
    }
    
    public DMGBlock(int blockType, int skipped, long outOffset, long outSize, long inOffset, long inSize) {
	this.blockType = blockType;
	this.skipped = skipped;
	this.outOffset = outOffset;
	this.outSize = outSize;
	this.inOffset = inOffset;
	this.inSize = inSize;
    }
    
    public static int structSize() { return 40; }
    
    public int getBlockType() { return blockType; }
    public int getSkipped() { return skipped; }
    public long getOutOffset() { return outOffset; }
    public long getOutSize() { return outSize; }
    public long getInOffset() { return inOffset; }
    public long getInSize() { return inSize; }
    
    public String getBlockTypeAsString() {
	switch(blockType) {
	case BT_ADC:
	    return "BT_ADC";
	case BT_ZLIB:
	    return "BT_ZLIB";
	case BT_BZIP2:
	    return "BT_BZIP2";
	case BT_COPY:
	    return "BT_COPY";
	case BT_ZERO:
	    return "BT_ZERO";
	case BT_ZERO2:
	    return "BT_ZERO2";
	case BT_END:
	    return "BT_END";
	case BT_UNKNOWN:
	    return "BT_UNKNOWN";
	default:
	    return "[Unknown block type! ID=0x" + Integer.toHexString(blockType) + "]";
	}
    }
    
    public void setOutOffsetCompensation(long offset) {
	outOffsetComp = offset;
    }
    public void setInOffsetCompensation(long offset) {
	inOffsetComp = offset;
    }
    public long getTrueOutOffset() {
	return outOffset+outOffsetComp;
    }
    public long getTrueInOffset() {
	return inOffset+inOffsetComp;
    }
    
    public String toString() {
	return getBlockTypeAsString() + 
	    "(skipped=0x" + Integer.toHexString(skipped) + ",outOffset=" + outOffset + 
	    ",outSize=" + outSize + ",inOffset=" + inOffset + ",inSize=" + inSize + ",outOffsetComp=" + outOffsetComp + ",inOffsetComp=" + inOffsetComp + ")";
    }
}
