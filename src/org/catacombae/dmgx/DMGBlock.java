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
    public int blockType;
    public int skipped;
    public long outOffset;
    public long outSize;
    public long inOffset;
    public long inSize;
    
    public DMGBlock(byte[] data, int offset) {
	DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
	int bytesSkipped = 0;
	while(bytesSkipped < offset)
	    bytesSkipped += dis.skipBytes(offset-bytesSkipped);
	
	this.blockType = util.readIntBE(data, offset+0);//dis.readInt();
	this.skipped = util.readIntBE(data, offset+4);//dis.readInt(); //Skip 4 bytes forward
	this.outOffset = util.readLongBE(data, offset+8)*0x200;//(dis.readInt() & 0xffffffffL)*0x200; //unsigned int -> long
	//dis.readInt(); //Skip 4 bytes forward
	this.outSize = util.readLongBE(data, offset+16)*0x200;//(dis.readInt() & 0xffffffffL)*0x200; //unsigned int -> long
	this.inOffset = util.readLongBE(data, offset+24);// & 0xffffffffL; //unsigned int -> long
	//dis.readInt(); //Skip 4 bytes forward
	this.inSize = util.readLongBE(data, offset+32);//dis.readInt() & 0xffffffffL; //unsigned int -> long
	
    }
    
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
