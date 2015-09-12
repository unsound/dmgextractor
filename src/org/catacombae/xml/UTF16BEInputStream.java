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

package org.catacombae.xml;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

/**
 * This class always outputs UTF-16BE encoded data. The input encoding
 * can be changed on the fly.
 */
public class UTF16BEInputStream extends InputStream {
    private InputStream underlyingStream;
    private Reader inReader;
    private CharsetEncoder utf16beEncoder;
    private int overflow = -1;
    private char[] tempArray = new char[4096];

    public UTF16BEInputStream(InputStream underlyingStream, String encodingName) throws UnsupportedEncodingException {
	this.underlyingStream = underlyingStream;
	this.inReader = new InputStreamReader(underlyingStream, encodingName);
	this.utf16beEncoder = Charset.forName("UTF-16BE").newEncoder();
    }
    
    public void changeEncoding(String encodingName) throws UnsupportedEncodingException {
	this.inReader = new InputStreamReader(underlyingStream, encodingName);
    }

    public int read() throws IOException {
	if(overflow != -1) {
	    int result = overflow;
	    overflow = -1;
	    return result;
	}
	else {
	    inReader.read(tempArray, 0, 1);
	    ByteBuffer bb = utf16beEncoder.encode(CharBuffer.wrap(tempArray, 0, 1));
	    int result = bb.get() & 0xFF;
	    overflow = bb.get() & 0xFF;
	    return result;
	}
    }
    
    @Override
    public int read(byte[] ba) throws IOException { return read(ba, 0, ba.length); }
    
    @Override
    public int read(byte[] ba, int offset, int length) throws IOException {
	if(length == 0) return 0;
	
	int curOffset = offset;
	int curLength = length;

	// Take care of the overflow, if any
	if(overflow != -1) {
	    ba[curOffset++] = (byte)overflow;
	    --length;
	    overflow = -1;
	}
	
	// Do the dance...
	int numberOfCharsToRead = length/2 + length%2;
	int charsRead = 0;
	while(numberOfCharsToRead > charsRead) {
	    int charsRemaining = numberOfCharsToRead - charsRead;
	    int curCharsRead = inReader.read(tempArray, 0, (charsRemaining < tempArray.length?charsRemaining:tempArray.length));
	    if(curCharsRead == -1)
		break;
	    else {
		CharBuffer cb = CharBuffer.wrap(tempArray, 0, curCharsRead);
		ByteBuffer bb = utf16beEncoder.encode(cb);
		int bytesToWrite = curCharsRead*2 > curLength?curLength:curCharsRead*2;
		bb.get(ba, curOffset, bytesToWrite);
		curOffset += bytesToWrite;
		curLength -= bytesToWrite;
		if(bytesToWrite != curCharsRead*2)
		    overflow = bb.get() & 0xFF;

		charsRead += curCharsRead;
		if(bytesToWrite != curCharsRead*2 && numberOfCharsToRead > charsRead)
		    throw new RuntimeException("Mind meltdown!");
	    }
	}
	
	if(numberOfCharsToRead*2 != length) {
	    if(numberOfCharsToRead*2-1 != length)
		throw new RuntimeException("wtf?!");
	    if(charsRead*2-1 == length)
		return length;
	    else
		return charsRead*2;
	}
	else
	    return charsRead*2;
    }
    
    @Override
    public long skip(long bytesToSkip) throws IOException {
	byte[] garbage = new byte[4096];
	long bytesRead = 0;
	while(bytesRead < bytesToSkip) {
	    int curBytesRead = read(garbage);
	    if(curBytesRead == -1)
		break;
	    else
		bytesRead += curBytesRead;
	}
	return bytesRead;
    }
    
    @Override
    public int available() throws IOException { return 0; }
    
    @Override
    public void close() throws IOException { underlyingStream.close(); }
    
    @Override
    public void mark(int readLimit) {}
    
    @Override
    public void reset() throws IOException { throw new IOException("Not supported"); }
    
    @Override
    public boolean markSupported() { return false; }
}