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

public class DmgRandomAccessStream implements RandomAccessStream {
    /*
      We have a string of data divided into blocks. Different algorithms must be applied to
      different types of blocks in order to extract the data.
     */
    private DmgFile dmgFile;
    private DMGBlock[] allBlocks;
    private DMGBlock currentBlock;
    private int currentBlockIndex;
    private DMGBlockInputStream currentBlockStream;
    
    private long length;
    /** This is the pointer to the current position in the virtual file provided by this stream. */
    private long logicalFilePointer = 0;
    private boolean seekCalled = false;
    
    private static void dbg(String s) { System.err.println(s); }
    
    public DmgRandomAccessStream(DmgFile dmgFile) throws IOException {
	this.dmgFile = dmgFile;
	//dbg("dmgFile.getView().getPlist(); free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
	Plist plist = dmgFile.getView().getPlist();
	//dbg("before gc(): free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
	//Runtime.getRuntime().gc();
	//dbg("plist.getPartitions(); free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
	DmgPlistPartition[] partitions = plist.getPartitions();

	int totalBlockCount = 0;
	for(DmgPlistPartition pp : partitions)
	    totalBlockCount += pp.getBlockCount();
	//dbg("totalBlockCount = " + totalBlockCount);
	
	allBlocks = new DMGBlock[totalBlockCount];
	int pos = 0;
	//dbg("looping for each of " + partitions.length + " partitions...");
	for(DmgPlistPartition pp : partitions) {
	    DMGBlock[] blocks = pp.getBlocks();
	    //dbg("Blocks in partition: " + blocks.length);
	    System.arraycopy(blocks, 0, allBlocks, pos, blocks.length);
	    pos += blocks.length;
	    length += pp.getPartitionSize();
	}
	if(totalBlockCount > 0) {
	    currentBlock = allBlocks[0];
	    currentBlockIndex = 0;
	    //dbg("Repositioning stream");
	    repositionStream();
	    //dbg("repositioning done.");
	}
	else
	    throw new RuntimeException("Could not find any blocks in the DMG file...");
    }
    
    /** @see java.io.RandomAccessFile */
    public void close() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException { return logicalFilePointer; }

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException { return length; }

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException {
	byte[] b = new byte[1];
	return read(b, 0, 1);
    }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException { return read(b, 0, b.length); }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException {
 	//System.out.println("DmgRandomAccessStream.read(b.length=" + b.length + ", " + off + ", " + len + ") {");
	if(seekCalled) {
	    seekCalled = false;
	    //System.out.print("  Repositioning stream after seek (logical file pointer: " + logicalFilePointer + ")...");
	    try { repositionStream(); }
	    catch(RuntimeException re) {
// 		System.out.println("return: -1 }");
		return -1;
	    }
	    //System.out.println("done.");
	}
	int bytesRead = 0;
	while(bytesRead < len) {
	    
	    int curBytesRead = currentBlockStream.read(b, off+bytesRead, len-bytesRead);
	    if(curBytesRead < 0) {
		//System.out.print("  Repositioning stream...");
		try { repositionStream(); }
		catch(RuntimeException re) {
		    if(bytesRead == 0)
			bytesRead = -1; // If no bytes could be read, we must indicate that the stream has no more data
		    break;
		}
		//System.out.println("done.");
		curBytesRead = currentBlockStream.read(b, off+bytesRead, len-bytesRead);
		if(curBytesRead < 0) {
		    throw new RuntimeException("No bytes could be read, and no exception was thrown! Program error...");
// 		    if(bytesRead == 0)
// 			bytesRead = -1; // If no bytes could be read, we must indicate that the stream has no more data
// 		    break;
		}
	    }
	    
// 	    if(curBytesRead >= 0)
	    bytesRead += curBytesRead;
	    logicalFilePointer += curBytesRead;
	}
	
	
	    
// 	System.out.println("return: " + bytesRead + " }");
	return bytesRead;
    }

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException {
	if(logicalFilePointer != pos) {
	    seekCalled = true;
	    logicalFilePointer = pos;
	}
    }
    
    private void repositionStream() throws IOException {
// 	System.out.println("<DmgRandomAccessStream.repositionStream()>");
	// if the global file pointer is not within the bounds of the current block, then find the accurate block
	if(!(currentBlock.getTrueOutOffset() <= logicalFilePointer &&
	     (currentBlock.getTrueOutOffset()+currentBlock.getOutSize()) > logicalFilePointer)) {
	    DMGBlock soughtBlock = null;
	    for(DMGBlock dblk : allBlocks) {
		long startPos = dblk.getTrueOutOffset();
		long endPos = startPos + dblk.getOutSize();
		if(startPos <= logicalFilePointer && endPos > logicalFilePointer) {
		    soughtBlock = dblk;
		    break;
		}
	    }
	    if(soughtBlock != null) {
		//System.out.println("REPOSITION " + currentBlock.getBlockTypeAsString() + "(" + currentBlock.getTrueOutOffset() + "," + currentBlock.getOutSize() + ") -> " + soughtBlock.getBlockTypeAsString() + "(" + soughtBlock.getTrueOutOffset() + "," + soughtBlock.getOutSize() + ")");
// 		if(soughtBlock.getTrueOutOffset() == currentBlock.getTrueOutOffset()+currentBlock.getOutSize())
// 		    System.out.println("  Continuous! :)");
// 		else
// 		    System.out.println("  FUCKADSFOA!!!1one");
		currentBlock = soughtBlock;
	    }
	    else
		throw new RuntimeException("Trying to seek outside bounds.");
	}
	
	currentBlockStream = DMGBlockInputStream.getStream(dmgFile.getStream(), currentBlock);
	long bytesToSkip = logicalFilePointer - currentBlock.getTrueOutOffset();
// 	System.out.print("  skipping " + bytesToSkip + " bytes...");
	currentBlockStream.skip(bytesToSkip);
// 	System.out.println("done.");
// 	System.out.println("</DmgRandomAccessStream.repositionStream()>");
    }
    
    public static void main(String[] args) throws IOException {
	System.out.println("DMGRandomAccessStream simple test program");
	System.out.println("(Simply extracts the contents of a DMG file to a designated output file)");
	if(args.length != 2)
	    System.out.println("  ERROR: You must supply exactly two arguments: 1. the DMG, 2. the output file");
	else {
	    byte[] buffer = new byte[4096];
	    DmgRandomAccessStream dras = 
		new DmgRandomAccessStream(new DmgFile(new RandomAccessFileStream(new RandomAccessFile(args[0], "r"))));
	    FileOutputStream fos = new FileOutputStream(args[1]);
	    
	    long totalBytesRead = 0;
	    
	    int bytesRead = dras.read(buffer);
	    while(bytesRead > 0) {
		totalBytesRead += bytesRead;
		fos.write(buffer, 0, bytesRead);
		bytesRead = dras.read(buffer);
	    }
	    System.out.println("Done! Extracted " + totalBytesRead + " bytes.");
	    System.out.println("Length: " + dras.length() + " bytes");
	}
    }
}
