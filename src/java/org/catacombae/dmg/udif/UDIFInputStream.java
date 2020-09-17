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

import java.io.*;

/**
 * An InputStream for reading the "block device" contents of a UDIF disk image file, usually
 * using the extension .dmg. Note that all files with the extension .dmg are not UDIF
 * encoded. Some are raw disk images, with no wrapper, and can be burnt or restored directly
 * to disk/optical disc.<br>
 * Make sure that no other stream concurrently uses the input file. This class isn't thread
 * safe, so use external synchronization if needed.
 */
public class UDIFInputStream extends InputStream {
    private UDIFRandomAccessStream wrapped;
    private long filePointer;
    
    /** Constructs a new UDIF input stream from a RandomAccessFile. This is a convenience method, and
	equal to <code>new UDI*/
    public UDIFInputStream(RandomAccessFile raf, String openPath)
            throws IOException
    {
        this(new UDIFRandomAccessStream(raf, openPath));
    }

    public UDIFInputStream(UDIFRandomAccessStream dras) throws IOException {
	this.wrapped = dras;
    }
    
    /** Returns the number of available bytes, or Integer.MAX_VALUE if the value is too large for int. */
    @Override
    public int available() throws IOException {
	long len = wrapped.length()-filePointer;
	if(len > Integer.MAX_VALUE)
	    return Integer.MAX_VALUE;
	else if(len < 0)
	    throw new IOException("Internal error! filePointer > wrapped.length! filePointer:" + filePointer + " wrapped.length():" + wrapped.length());
	else
	    return (int)len;
    }
    /** Does nothing. You will have to close the underlying RandomAccessFile or RandomAccessStream manually. */
    @Override
    public void close() throws IOException {}
    
    /** Not supported. */
    @Override
    public void mark(int readlimit) {}
    
    /** Mark is not supported. This method always returns false. */
    @Override
    public boolean markSupported() { return false; }
    
    /** See the general contract of the read method for java.io.InputStream. */
    @Override
    public int read() throws IOException {
	byte[] b = new byte[1];
	if(read(b, 0, 1) != 1)
	    return -1;
	else
	    return b[0] & 0xFF;
    }
    
    /** See the general contract of the read method for java.io.InputStream. */
    @Override
    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }
    
    /** See the general contract of the read method for java.io.InputStream. */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
	if(wrapped.getFilePointer() != filePointer)
	    wrapped.seek(filePointer);
	int bytesRead = wrapped.read(b, off, len);
	if(bytesRead > 0)
	    filePointer += bytesRead;
	return bytesRead;
    }
    
    /** Not supported. */
    @Override
    public void reset() throws IOException {}
    
    /** See the general contract of the skip method for java.io.InputStream. */
    @Override
    public long skip(long n) throws IOException {
	if(n < 0) throw new IllegalArgumentException("n must be positive");
	long newPos = filePointer+n;
	if(newPos > wrapped.length())
	    newPos = wrapped.length();
	wrapped.seek(newPos);
	long result = newPos - filePointer;
	filePointer = newPos;
	return result;
    }
    
    /** Test code. */
    public static void main(String[] args) throws IOException {
	if(args.length != 2)
	    System.out.println("usage: java org.catacombae.udif.UDIFInputStream <infile> <outfile>");
	File inFile = new File(args[0]);
	File outFile = new File(args[1]);
	
	RandomAccessFile inRaf = null;
	FileOutputStream outFos = null;
	
	if(inFile.canRead())
	    inRaf = new RandomAccessFile(inFile, "r");
	else {
	    System.out.println("Can't read from input file!");
	    System.exit(0);
	}
	
	if(!outFile.exists())
	    outFos = new FileOutputStream(outFile);
	else {
	    System.out.println("Output file already exists!");
	    System.exit(0);
	}
	
        UDIFInputStream dis = new UDIFInputStream(inRaf, inFile.getPath());
	byte[] buffer = new byte[8192];
	long bytesExtracted = 0;
	int bytesRead = dis.read(buffer);
	while(bytesRead > 0) {
	    bytesExtracted += bytesRead;
	    outFos.write(buffer, 0, bytesRead);
	    bytesRead = dis.read(buffer);
	}
	dis.close();
	inRaf.close();
	outFos.close();
	
	System.out.println("Extracted " + bytesExtracted + " bytes to \"" + outFile + "\".");
    }
}
