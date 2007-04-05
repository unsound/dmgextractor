/*-
 * Copyright (C) 2006-2007 Erik Larsson
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */

package org.catacombae.dmgx;

import org.catacombae.io.*;
import java.io.*;
import java.util.zip.*;

public abstract class DMGBlockInputStream extends InputStream {
    protected RandomAccessStream raf;
    protected DMGBlock block;
    protected final int addInOffset;
    private long globalBytesRead;
    protected final byte[] buffer = new byte[16384]; // 16 KiB buffer... is it reasonable?
    protected int bufferPos = 0;
    protected int bufferDataLength = 0; // Initializing this to zero will make read call fillBuffer at first call
    
    private final byte[] skipBuffer = new byte[4096];
    
    protected int fillSize; // Subclasses use this variable to report how many bytes were read into the buffer
    
    /**
     * Creates a new DMGBlockInputStream.
     * @param raf the RandomAccessFile representing the DMG file
     * @param block the block that we should read (usually obtained via DmgPlistPartition.getBlocks())
     * @param addInOffset the number to add to the block's inOffset to find the data
     */
    protected DMGBlockInputStream(RandomAccessStream raf, DMGBlock block, int addInOffset) throws IOException {
	this.raf = raf;
	this.block = block;
	this.addInOffset = addInOffset;
	//fillBuffer();
	//bufferPos = buffer.length;
    }
    
    /** This method WILL throw a RuntimeException if <code>block</code> has a type that there is no handler for. */
    public static DMGBlockInputStream getStream(RandomAccessStream raf, DMGBlock block) throws IOException {
	switch(block.getBlockType()) {
	case DMGBlock.BT_ZLIB:
	    return new ZlibBlockInputStream(raf, block, 0);
	case DMGBlock.BT_COPY:
	    return new CopyBlockInputStream(raf, block, 0);
	case DMGBlock.BT_ZERO:
 	case DMGBlock.BT_ZERO2:
	    return new ZeroBlockInputStream(raf, block, 0);
	case DMGBlock.BT_END:
 	case DMGBlock.BT_UNKNOWN:
	    throw new RuntimeException("Block type is a marker and contains no data.");
	case DMGBlock.BT_ADC:
	case DMGBlock.BT_BZIP2:
	default:
	    throw new RuntimeException("No handler for block type " + block.getBlockTypeAsString());
  	}
    }
    
    /** In case the available amount of bytes is larger than Integer.MAX_INT, Integer.MAX_INT is returned. */
    public int available() throws IOException {
	long available = block.getOutSize()-globalBytesRead;
	if(available > Integer.MAX_VALUE)
	    return Integer.MAX_VALUE;
	else
	    return (int)available;
    }
    /** This method does NOT close the underlying RandomAccessFile. It can be reused afterwards. */
    public void close() throws IOException {}
    /** Not supported. */
    public void mark(int readlimit) {}
    /** Returns false, beacuse it isn't supported. */
    public boolean markSupported() { return false; }

    /** @see java.io.InputStream */
    public int read() throws IOException {
	byte[] b = new byte[1];
	return read(b, 0, 1);
    }
    
    /** @see java.io.InputStream */
    public int read(byte[] b) throws IOException { return read(b, 0, b.length); }
    
    /** @see java.io.InputStream */
    public int read(byte[] b, int off, int len) throws IOException {
// 	System.out.println("DMGBlockInputStream.read(b, " + off + ", " + len + ") {");

	final int bytesToRead = len;
	
	int bytesRead = 0;
	int outPos = off;
	while(bytesRead < bytesToRead) {
	    int bytesRemainingInBuffer = bufferDataLength - bufferPos;
	    if(bytesRemainingInBuffer == 0) {
// 		System.out.println("  first call to fillBuffer");
		fillBuffer();
// 		System.out.println("  bufferDataLength=" + bufferDataLength + ",bufferPos=" + bufferPos);
		bytesRemainingInBuffer = bufferDataLength - bufferPos;
		if(bytesRemainingInBuffer == 0) { // We apparently have no more data.
		    if(bytesRead == 0) {
			//System.out.println("return: -1 }");
			return -1;
		    }
		    else break;
		}
	    }
// 	    System.out.println("  bytesRemainingInBuffer=" + bytesRemainingInBuffer + ",bufferPos=" + bufferPos + ",bufferDataLength=" + bufferDataLength);
	    int bytesToReadFromBuffer = ((bytesToRead-bytesRead < bytesRemainingInBuffer) ? bytesToRead-bytesRead : bytesRemainingInBuffer);
// 	    System.out.println("  bytesToReadFromBuffer=" + bytesToReadFromBuffer);
// 	    System.out.println("  System.arraycopy(buffer, " + bufferPos + ", b, " + outPos + ", " + bytesToReadFromBuffer + ");");
	    System.arraycopy(buffer, bufferPos, b, outPos, bytesToReadFromBuffer);
	    
	    outPos += bytesToReadFromBuffer;
	    bufferPos += bytesToReadFromBuffer;
	    
	    bytesRead += bytesToReadFromBuffer;
	}
	
	globalBytesRead += bytesRead;

// 	System.out.println("return: " + bytesRead + " }");
	return bytesRead;
    }
    /** Does nothing. Not supported. */
    public void reset() throws IOException {}
    /** Skips as many bytes as possible. If end of file is reached, the number of bytes skipped is returned. */
    public long skip(long n) throws IOException {
	long bytesSkipped = 0;
	while(bytesSkipped < n) {
	    int curSkip = (n-bytesSkipped < skipBuffer.length)?(int)(n-bytesSkipped):skipBuffer.length;
	    int res = read(skipBuffer, 0, curSkip);
	    if(res > 0)
		bytesSkipped += res;
	    else
		break;
	}
	return bytesSkipped;
    }
    
    protected abstract void fillBuffer() throws IOException;
    
    public static class ZlibBlockInputStream extends DMGBlockInputStream {
	//private static byte[] inBuffer = new byte[0x40000];
	//private static byte[] outBuffer = new byte[0x40000];
	private final Inflater inflater;
	private final byte[] inBuffer;
	private long inPos;
	
	public ZlibBlockInputStream(RandomAccessStream raf, DMGBlock block, int addInOffset) throws IOException {
	    super(raf, block, addInOffset);
	    inflater = new Inflater();
	    inBuffer = new byte[4096];
	    inPos = 0;
// 	    if(inflater == null)
// 		System.err.println("INFLATER IS NULL");
// 	    else
// 		System.err.println("inflater is " + inflater);
// 	    if(inBuffer == null)
// 		System.err.println("INBUFFER IS NULL");
// 	    else
// 		System.err.println("inBuffer is " + inBuffer);
	    feedInflater();
	}
	private void feedInflater() throws IOException {
	    //System.out.println("ZlibBlockInputStream.feedInflater() {");
	    long seekPos = addInOffset+inPos+block.getTrueInOffset();
	    //System.out.println("  seeking to " + seekPos + " (file length: " + raf.length() + ")");
	    raf.seek(seekPos);
	    long bytesLeftToRead = block.getInSize()-inPos;
	    int bytesToFeed = (inBuffer.length<bytesLeftToRead) ? inBuffer.length : (int)bytesLeftToRead;
	    int curBytesRead = raf.read(inBuffer, 0, bytesToFeed);
	    inPos += curBytesRead;
	    inflater.setInput(inBuffer, 0, curBytesRead);
	    //System.out.println("  curBytesRead=" + curBytesRead);
	    //System.out.println("}");
	}
	
	protected void fillBuffer() throws IOException {
	    //System.out.println("ZlibBlockInputStream.fillBuffer() {");
// 	    if(inflater == null)
// 		System.err.println("INFLATER IS NULL");
// 	    if(inBuffer == null)
// 		System.err.println("INBUFFER IS NULL");
	    if(inflater.finished()) {
		//System.out.println("inflater claims to be finished...");
		bufferPos = 0;
		bufferDataLength = 0;
	    }
	    try {
		int bytesInflated = 0;
		while(bytesInflated < buffer.length && !inflater.finished()) {
		    if(inflater.needsInput())
			feedInflater();
		    int res = inflater.inflate(buffer, bytesInflated, buffer.length-bytesInflated);
		    if(res >= 0)
			bytesInflated += res;
		    else
			throw new RuntimeException("Negative return value when inflating");
		}
		
		// The fillBuffer method is responsible for updating bufferPos and bufferDataLength
		bufferPos = 0;
		bufferDataLength = bytesInflated;
	    } catch(DataFormatException e) {
		IOException ioe = new IOException("Invalid zlib data!");
		ioe.initCause(e);
		throw ioe;
	    }
	    //System.out.println("}");
	}
    }
    public static class CopyBlockInputStream extends DMGBlockInputStream {
	private long inPos = 0;
	public CopyBlockInputStream(RandomAccessStream raf, DMGBlock block, int addInOffset) throws IOException {
	    super(raf, block, addInOffset);
	}
	
	protected void fillBuffer() throws IOException {
	    raf.seek(addInOffset+inPos+block.getTrueInOffset());
	    
	    final int bytesToRead = (int)Math.min(block.getInSize()-inPos, buffer.length);
	    int totalBytesRead = 0;
	    while(totalBytesRead < bytesToRead) {
		int bytesRead = raf.read(buffer, 0, bytesToRead-totalBytesRead);
		if(bytesRead < 0)
		    break;
		else {
		    totalBytesRead += bytesRead;
		    inPos += bytesRead;
		}
	    }
	    
	    // The fillBuffer method is responsible for updating bufferPos and bufferDataLength
	    bufferPos = 0;
	    bufferDataLength = totalBytesRead;
	}
    }
    public static class ZeroBlockInputStream extends DMGBlockInputStream {
	private long outPos = 0;
	public ZeroBlockInputStream(RandomAccessStream raf, DMGBlock block, int addInOffset) throws IOException {
	    super(raf, block, addInOffset);
	}
	
	protected void fillBuffer() throws IOException {
	    final int bytesToWrite = (int)Math.min(block.getOutSize()-outPos, buffer.length);
	    Util.zero(buffer, 0, bytesToWrite);
	    outPos += bytesToWrite;
	    
	    // The fillBuffer method is responsible for updating bufferPos and bufferDataLength
	    bufferPos = 0;
	    bufferDataLength = bytesToWrite;
	}
    }
}
