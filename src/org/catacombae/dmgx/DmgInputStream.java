/*-
 * Copyright (C) 2007 Erik Larsson
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.dmgx;

import org.catacombae.io.*;
import java.io.*;

/**
 * An InputStream outputting the contents of a DMG UDIF file.
 */
public class DmgInputStream extends InputStream {
    private RandomAccessFile raf;
    private DmgFileView dmgView;
    private Plist plist;
    private DMGBlock[] allBlocks;
    private int currentBlock;
    private long totalReadableBytes;
    private long bytesRead;
    
    public DmgInputStream(RandomAccessFile raf) throws IOException {
	this.raf = raf;
	this.dmgView = new DmgFileView(new RandomAccessFileStream(raf));
	this.plist = dmgView.getPlist();
	
	DmgPlistPartition[] parts = plist.getPartitions();
	int totalNumBlocks = 0;
	for(DmgPlistPartition dpp : parts)
	    totalNumBlocks += dpp.getBlockCount();
	
	this.allBlocks = new DMGBlock[totalNumBlocks];
	int blockPtr = 0;
	for(DmgPlistPartition dpp : parts) {
	    for(DMGBlock block : dpp.getBlocks())
		allBlocks[blockPtr++] = block;
	}
	
	this.currentBlock = 0;
	
	this.totalReadableBytes = 0;
	for(DMGBlock b : allBlocks)
	    totalReadableBytes += b.getOutSize();
	
	this.bytesRead = 0;
    }
    
    public int available() throws IOException {
	long available = totalReadableBytes;
	available -= bytesRead;
	if(available > Integer.MAX_VALUE)
	    return Integer.MAX_VALUE;
	else
	    return (int)available;
    }
    /* IMPLEMENT: */
    public void close() throws IOException {}
    public void mark(int readlimit) {}
    public boolean markSupported() { return false; }
    public int read() throws IOException { return -1; }
    public int read(byte[] b) throws IOException { return -1; }
    public int read(byte[] b, int off, int len) throws IOException { return -1; }
    public void reset() throws IOException {}
    public long skip(long n) throws IOException { return -1; }
}
