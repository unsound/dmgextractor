/*-
 * Copyright (C) 2007-2008 Erik Larsson
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

package org.catacombae.dmgextractor.io;
import java.io.*;

/** Records information about what has been read. */
public class ByteCountInputStream extends InputStream {
    private long bytePos = 0;
    private InputStream is;
    
    public ByteCountInputStream(InputStream is) {
	this.is = is;
    }
    public long getBytesRead() { return bytePos; }
    
    /** @see java.io.InputStream */
    @Override
    public int available() throws IOException { return is.available(); }

    /** @see java.io.InputStream */
    @Override
    public void close() throws IOException { is.close(); }

    /** @see java.io.InputStream */
    @Override
    public void mark(int readLimit) { throw new RuntimeException("Mark/reset not supported"); }
    
    /** @see java.io.InputStream */
    @Override
    public boolean markSupported() { return false; }

    /** @see java.io.InputStream */
    @Override
    public int read() throws IOException {
	//System.out.println("read();");
	int res = is.read();
	if(res > 0)
	    ++bytePos;
	return res;
    }

    /** @see java.io.InputStream */
    @Override
    public int read(byte[] b) throws IOException {
	//System.out.println("read(b.length=" + b.length + ");");
	int res = is.read(b);
	if(res > 0) bytePos += res;
	return res;
    }

    /** @see java.io.InputStream */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
	//System.out.println("read(b.length=" + b.length + ", " + off + ", " + len + ");");
	int res = is.read(b, off, len);
	if(res > 0) bytePos += res;
	return res;
    }

    /** @see java.io.InputStream */
    @Override
    public void reset() throws IOException { throw new RuntimeException("Mark/reset not supported"); }

    /** @see java.io.InputStream */
    @Override
    public long skip(long n) throws IOException {
	System.out.println("skip(" + n + ");");
	long res = is.skip(n);
	if(res > 0) bytePos += res;
	return res;
    }
}
