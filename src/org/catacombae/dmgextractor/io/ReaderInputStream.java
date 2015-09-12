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

import java.io.*;
import java.nio.charset.*;

public class ReaderInputStream extends InputStream {
    private Reader r;
    private CharsetEncoder encoder;
    private byte[] chardata;
    private int remainingChardata = 0;
    private LousyByteArrayStream lbas;
    private OutputStreamWriter osw;
    
    private static class LousyByteArrayStream extends OutputStream {
	private final byte[] buffer;
	private int bufpos = 0;
	public LousyByteArrayStream(int buflen) {
	    //System.err.println("Creating a LousyByteArrayStream with length " + buflen);
	    buffer = new byte[buflen];
	}
	public void write(int b) {
	    buffer[bufpos++] = (byte)b;
	}
	public int reset(byte[] chardata) {
	    int length = bufpos;
	    System.arraycopy(buffer, 0, chardata, 0, length);
	    bufpos = 0;
	    return length;
	}
    }
    
    public ReaderInputStream(Reader r, Charset c) {
	this.r = r;
	this.encoder = c.newEncoder();
	//System.err.println("Creating a ReaderInputStream. encoder.maxBytesPerChar() == " + encoder.maxBytesPerChar());
	this.chardata = new byte[(int)Math.ceil(encoder.maxBytesPerChar())];

	lbas = new LousyByteArrayStream(chardata.length);
	osw = new OutputStreamWriter(lbas, encoder);

    }

    public int read() throws IOException {
	byte[] b = new byte[1];
	int res = read(b, 0, 1);
	if(res == 1)
	    return b[0] & 0xFF;
	else
	    return -1;
    }
    
    @Override
    public int read(byte[] b) throws IOException { return read(b, 0, b.length); }

    /* Säg då att vi skippar 204 bytes. Vad händer? När vi går in i read(3) är remainingChardata = 0,
       och off = 0, len = 204. b.length = 4096. Alltså kommer ingen av de 4 första if-satserna vara
       giltiga...
       I vilken situation kan vi ha läst in mindre data i b än returvärdet?
    */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
// 	System.err.println("ReaderInputStream.read(b.length=" + b.length + ", " + off + ", " + len + ")");
	if(len < 0) throw new IllegalArgumentException();
	if(len == 0) return 0;
	
	int originalOffset = off;
	int endPos = off+len;
	
	if(remainingChardata > 0) {
// 	    System.err.println("Remaining chardata! length=" + remainingChardata);
	    int bytesToCopy = remainingChardata>len?len:remainingChardata;
// 	    System.err.println("bytesToCopy=" + bytesToCopy);
	    System.arraycopy(chardata, 0, b, off, bytesToCopy);
	    off += bytesToCopy;
	    remainingChardata -= bytesToCopy;
	}
	if(off == endPos) {
// 	    System.err.println("(1)returning with " + (off-originalOffset) + " from ReaderInputStream.read");
	    return off-originalOffset;
	}	
	
// 	int baba = 3;
	while(off < endPos) {
// 	    if(baba > 0) {
// 		System.err.println("  looping... off==" + off + " endPos=" + endPos);
// 		--baba;
// 	    }
// 	    else
// 		baba = Integer.MIN_VALUE;
	    int cur = r.read();
	    if(cur < 0)
		break;
	    
	    if(Character.isHighSurrogate((char)cur)) {
		int lowSurrogate = r.read(); // UTF-16 is a crap encoding for a programming language
		
		if(lowSurrogate < 0)
		    throw new IOException("Too lazy to handle this error...");
		else if(!Character.isSurrogatePair((char)cur, (char)lowSurrogate))
		    throw new IOException("Encountered a high surrogate without a matching low surrogate... oh crap.");
		
		cur = Character.toCodePoint((char)cur, (char)lowSurrogate);
	    }
	    char[] charArray = Character.toChars(cur);
	    String charString = new String(charArray, 0, charArray.length);
	    
	    // Now we need to write
	    //System.out.println("Writing codepoint: 0x" + Util.toHexStringBE(cur));
	    
	    osw.write(charString);
	    osw.flush();
	    
	    //System.out.println("Resetting...");
	    int chardataLength = lbas.reset(chardata);
	    int remainingLength = endPos-off;
	    int bytesToCopy = (chardataLength > remainingLength)?remainingLength:chardataLength;
	    System.arraycopy(chardata, 0, b, off, bytesToCopy);
	    off += bytesToCopy;
	    
	    if(chardataLength > remainingLength) {
		remainingChardata = chardataLength-remainingLength;
		System.arraycopy(chardata, bytesToCopy, chardata, 0, remainingChardata);
	    }
// 	    if(baba >= 0) {
// 		System.err.println("  chardataLength=" + chardataLength + " remainingLength=" + remainingLength);
// 		System.err.println("  bytesToCopy=" + bytesToCopy + " off=" + off);
// 	    }
	}
	int bytesRead = off-originalOffset;
	if(off < endPos && bytesRead == 0) { // We have a break due to end of stream
	    //System.err.println("(3)returning -1 due to end of stream");
	    return -1;
	}
	else {
	    //System.err.println("(2)returning with " + bytesRead + " from ReaderInputStream.read");
	    return bytesRead;
	}
    }
    
    @Override
    public long skip(long n) throws IOException {
	System.err.println("ReaderInputStream.skip(" + n + ")");
	byte[] skipBuffer = new byte[4096];
	long bytesSkipped = 0;
	while(bytesSkipped < n) {
	    //System.out.println("  Looping...");
	    long remainingBytes = n-bytesSkipped;
	    int bytesToSkip = (int)(skipBuffer.length<remainingBytes ? skipBuffer.length : remainingBytes);
	    //System.out.println("  Skipping " + bytesToSkip + " this iteration.");
	    int res = read(skipBuffer, 0, bytesToSkip);
	    if(res > 0) {
		//System.out.println("  Actually skipped " + res + " bytes.");
		//System.out.println(" This is the data skipped: " + Util.byteArrayToHexString(skipBuffer, 0, res));
		bytesSkipped += res;
	    }
	    else {
		//System.out.println("encountered EOF!");
		break; // Seems we can't skip all bytes
	    }
	}
	//debug
	//System.out.println(" bytesSkipped=" + bytesSkipped + " n=" + n);
	return bytesSkipped; //super.skip(n);
    }
}
