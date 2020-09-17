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

package org.catacombae.dmgextractor.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Filter stream that records information about how many bytes have been
 * processed (either read or skipped).
 */
public class ByteCountInputStream extends InputStream {
    private long bytePos = 0;
    private InputStream is;

    /**
     * Creates a new ByteCountInputStream wrapping <code>is</code>.
     *
     * @param is the underlying InputStream.
     */
    public ByteCountInputStream(InputStream is) {
        this.is = is;
    }

    /**
     * Returns the number of bytes that have been read since the creation
     * of this filter stream.
     *
     * @return the number of bytes that have been read.
     */
    public long getBytesRead() { return bytePos; }
    
    /** {@inheritDoc} */
    @Override
    public int available() throws IOException { return is.available(); }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException { is.close(); }

    /** {@inheritDoc} */
    @Override
    public void mark(int readLimit) { throw new UnsupportedOperationException("Mark/reset not supported"); }
    
    /** {@inheritDoc} */
    @Override
    public boolean markSupported() { return false; }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        //System.out.println("read();");
        int res = is.read();
        if(res > 0)
            ++bytePos;
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] b) throws IOException {
        //System.out.println("read(b.length=" + b.length + ");");
        int res = is.read(b);
        if(res > 0)
            bytePos += res;
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        //System.out.println("read(b.length=" + b.length + ", " + off + ", " + len + ");");
        int res = is.read(b, off, len);
        if(res > 0)
            bytePos += res;
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() throws IOException { throw new UnsupportedOperationException("Mark/reset not supported"); }

    /** {@inheritDoc} */
    @Override
    public long skip(long n) throws IOException {
        System.out.println("skip(" + n + ");");
        long res = is.skip(n);
        if(res > 0)
            bytePos += res;
        return res;
    }
}
