/*-
 * Copyright (C) 2007-2008 Erik Larsson
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.catacombae.io.BasicReadableRandomAccessStream;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;


public class UDIFRandomAccessStream extends BasicReadableRandomAccessStream {
    /*
      We have a string of data divided into blocks. Different algorithms must be applied to
      different types of blocks in order to extract the data.
     */
    private UDIFFile dmgFile;
    private UDIFBlock[] allBlocks;
    private UDIFBlock currentBlock;
    private UDIFBlockInputStream currentBlockStream;
    
    private long length;
    /** This is the pointer to the current position in the virtual file provided by this stream. */
    private long logicalFilePointer = 0;
    private boolean seekCalled = false;
    
    private static void dbg(String s) { System.err.println(s); }
    
    public UDIFRandomAccessStream(RandomAccessFile raf, String openPath)
            throws RuntimeIOException
    {
        this(new ReadableFileStream(raf, openPath));
    }
    
    public UDIFRandomAccessStream(ReadableRandomAccessStream stream) throws RuntimeIOException {
	this(new UDIFFile(stream));
    }
    
    public UDIFRandomAccessStream(UDIFFile dmgFile) throws RuntimeIOException {
	this.dmgFile = dmgFile;
	//dbg("dmgFile.getView().getPlist(); free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
	Plist plist = dmgFile.getView().getPlist();
	//dbg("before gc(): free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
	//Runtime.getRuntime().gc();
	//dbg("plist.getPartitions(); free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
        try {
            PlistPartition[] partitions = plist.getPartitions();

            int totalBlockCount = 0;
            for(PlistPartition pp : partitions) {
                totalBlockCount += pp.getBlockCount();
            //dbg("totalBlockCount = " + totalBlockCount);
            }
            allBlocks = new UDIFBlock[totalBlockCount];
            int pos = 0;
            //dbg("looping for each of " + partitions.length + " partitions...");
            for(PlistPartition pp : partitions) {
                UDIFBlock[] blocks = pp.getBlocks();
                //dbg("Blocks in partition: " + blocks.length);
                System.arraycopy(blocks, 0, allBlocks, pos, blocks.length);
                pos += blocks.length;
                length += pp.getPartitionSize();
            }
            if(totalBlockCount > 0) {
                currentBlock = allBlocks[0];
                //dbg("Repositioning stream");
                repositionStream();
            //dbg("repositioning done.");
            }
            else {
                throw new RuntimeException("Could not find any blocks in the DMG file...");
            }
        } catch(IOException ex) {
            throw new RuntimeIOException(ex);
        }
    }
    
    /** @see java.io.RandomAccessFile */
    public void close() throws RuntimeIOException {}

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws RuntimeIOException { return logicalFilePointer; }

    /** @see java.io.RandomAccessFile */
    public long length() throws RuntimeIOException { return length; }

    /** @see java.io.RandomAccessFile */
    public int read() throws RuntimeIOException {
	byte[] b = new byte[1];
	if(read(b, 0, 1) != 1)
	    return -1;
	else
	    return b[0] & 0xFF;
    }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws RuntimeIOException { return read(b, 0, b.length); }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws RuntimeIOException {
        try {
 	//System.out.println("UDIFRandomAccessStream.read(b.length=" + b.length + ", " + off + ", " + len + ") {");
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
        } catch(IOException ex) {
            throw new RuntimeIOException(ex);
        }
    }

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws RuntimeIOException {
	if(logicalFilePointer != pos) {
	    seekCalled = true;
	    logicalFilePointer = pos;
	}
    }
    
    private void repositionStream() throws RuntimeIOException {
// 	System.out.println("<UDIFRandomAccessStream.repositionStream()>");
        try {
	// if the global file pointer is not within the bounds of the current block, then find the accurate block
	if(!(currentBlock.getTrueOutOffset() <= logicalFilePointer &&
	     (currentBlock.getTrueOutOffset()+currentBlock.getOutSize()) > logicalFilePointer)) {
	    UDIFBlock soughtBlock = null;
	    for(UDIFBlock dblk : allBlocks) {
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
	
	currentBlockStream = UDIFBlockInputStream.getStream(dmgFile.getStream(), currentBlock);
	long bytesToSkip = logicalFilePointer - currentBlock.getTrueOutOffset();
// 	System.out.print("  skipping " + bytesToSkip + " bytes...");
	currentBlockStream.skip(bytesToSkip);
// 	System.out.println("done.");
        } catch(IOException ex) {
            throw new RuntimeIOException(ex);
        }
// 	System.out.println("</UDIFRandomAccessStream.repositionStream()>");
    }
    
    public static void main(String[] args) throws IOException {
	System.out.println("UDIFRandomAccessStream simple test program");
	System.out.println("(Simply extracts the contents of a DMG file to a designated output file)");
	if(args.length != 2)
	    System.out.println("  ERROR: You must supply exactly two arguments: 1. the DMG, 2. the output file");
	else {
	    byte[] buffer = new byte[4096];
	    UDIFRandomAccessStream dras = 
                new UDIFRandomAccessStream(new UDIFFile(new ReadableFileStream(
                new RandomAccessFile(args[0], "r"), args[0])));
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
