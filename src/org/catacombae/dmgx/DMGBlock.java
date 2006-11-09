package org.catacombae.dmgx;

public class DMGBlock {
    /*
     * 4
     * 4
     * 8
     * 8
     * 8
     * 8
     * ---
     * 40 / 0x28 bytes
     */
    public int blockType;
    public int skipped;
    public long outOffset;
    public long outSize;
    public long inOffset;
    public long inSize;
    
    public DMGBlock(int blockType, int skipped, long outOffset, long outSize, long inOffset, long inSize) {
	this.blockType = blockType;
	this.skipped = skipped;
	this.outOffset = outOffset;
	this.outSize = outSize;
	this.inOffset = inOffset;
	this.inSize = inSize;
    }
    
    public String toString() {
	return "[type: 0x" + Integer.toHexString(blockType) + " skipped: 0x" + Integer.toHexString(skipped) + " outOffset: " + outOffset + " outSize: " + outSize + " inOffset: " + inOffset + " inSize: " + inSize + "]";
    }
}
