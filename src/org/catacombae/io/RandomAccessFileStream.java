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

public class RandomAccessFileStream implements RandomAccessStream {
    private RandomAccessFile raf;
    
    public RandomAccessFileStream(RandomAccessFile raf) throws FileNotFoundException {
	this.raf = raf;
    }
    
    /** @see java.io.RandomAccessFile */
    public void close() throws IOException {
	raf.close();
    }

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException {
	return raf.getFilePointer();
    }

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException {
	return raf.length();
    }

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException {
	return raf.read();
    }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException {
	return raf.read(b);
    }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException {
	return raf.read(b, off, len);
    }

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException {
	raf.seek(pos);
    }
}
