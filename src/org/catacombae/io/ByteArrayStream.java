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

package org.catacombae.io;
import java.io.*;

public class ByteArrayStream implements RandomAccessStream {
    private final byte[] data;
    private final int startOffset;
    private final int length;
    private int currentOffset;
    
    public ByteArrayStream(byte[] b) {
	this(b, 0, b.length);
    }
    public ByteArrayStream(byte[] b, int off, int len) {
	if(off < 0 || off+len > b.length)
	    throw new IllegalArgumentException("off (" + off + ") and/or len (" + len + ") out of range for b (b.length=" + b.length + ")");
	this.data = b;
	this.startOffset = off;
	this.length = len;
	this.currentOffset = startOffset;
    }
    /** Doesn't do anything. */
    public void close() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException {
	return currentOffset-startOffset;
    }

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException {
	return length;
    }

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException {
	byte[] ba = new byte[1];
	int res = read(ba, 0, 1);
	if(res == 1)
	    return ba[0] & 0xFF;
	else
	    return -1;
    }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException { return read(b, 0, b.length); }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException {
	int bytesToRead = (currentOffset+len > data.length?data.length-currentOffset:len);
	if(bytesToRead == 0)
	    return -1;
	else {
	    System.arraycopy(data, currentOffset, b, off, bytesToRead);
	    currentOffset += bytesToRead;
	    return bytesToRead;
	}
    }

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException {
	if(pos < 0 || pos > length)
	    throw new IllegalArgumentException("out of bounds");
	else
	    currentOffset = (int)pos;
    }
}
