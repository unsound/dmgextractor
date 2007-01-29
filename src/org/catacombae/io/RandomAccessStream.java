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

package org.catacombae.io;

import java.io.*;

/**
 * Designed to mimic RandomAccessFile but removing the limitation that
 * all data has to be stored in one physical file on the disk. The
 * stream may be composed of multiple files, be read from memory etc.
 * It's all implementation dependent. The stream must however be
 * seekable, and have a known limited length.
 */
public interface RandomAccessStream {

    /** @see java.io.RandomAccessFile */
    public void close() throws IOException;

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException;

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException;

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException;

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException;

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException;

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException;
}
