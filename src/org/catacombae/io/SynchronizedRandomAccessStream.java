/*-
 * Copyright (C) 2006 Erik Larsson
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

/**
 * This class adds concurrency safety to a random access stream. It includes a seek+read
 * atomic operation. All operations on this object is synchronized on its own monitor.
 */
public class SynchronizedRandomAccessStream implements RandomAccessStream {
    /** The underlying stream. */
    private RandomAccessStream ras;
    
    public SynchronizedRandomAccessStream(RandomAccessStream ras) {
	this.ras = ras;
    }
     
    /** Atomic seek+read. */
    public synchronized int readFrom(long pos, byte[] b, int off, int len) throws IOException {
	if(getFilePointer() != pos)
	    seek(pos);
	return read(b, off, len);
    }
    
    /** Atomic seek+skip. */
    public synchronized long skipFrom(final long pos, final long length) throws IOException {
	long streamLength = length();
	long newPos = pos+length;

	if(newPos > streamLength) {
	    seek(streamLength);
	    return streamLength-pos;
	}
	else {
	    seek(newPos);
	    return length;
	}
    }
    
    /** Atomic length() - getFilePointer(). */
    public synchronized long remainingLength() throws IOException {
	return length()-getFilePointer();
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized void close() throws IOException {
	ras.close();
    }

    /** @see java.io.RandomAccessFile */
    public synchronized long getFilePointer() throws IOException {
	return ras.getFilePointer();
    }

    /** @see java.io.RandomAccessFile */
    public synchronized long length() throws IOException {
	return ras.length();
    }

    /** @see java.io.RandomAccessFile */
    public synchronized int read() throws IOException {
	return ras.read();
    }

    /** @see java.io.RandomAccessFile */
    public synchronized int read(byte[] b) throws IOException {
	return ras.read(b);
    }

    /** @see java.io.RandomAccessFile */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
	return ras.read(b, off, len);
    }

    /** @see java.io.RandomAccessFile */
    public synchronized void seek(long pos) throws IOException {
	ras.seek(pos);
    }
}
