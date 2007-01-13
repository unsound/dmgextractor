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
