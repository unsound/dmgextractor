package org.catacombae.io;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class CharByCharReader extends Reader {
    private InputStream is;
    private Charset cs;
    private CharsetDecoder cdec;
    private byte[] tempBuffer;
    private int tempBufferPtr = 0;
    
    //private CharArrayBuilder cab = new CharArrayBuilder();
    
    /* The assumption we make here is that a number of bytes define a Unicode character. */
    public CharByCharReader(InputStream is, Charset cs) {
	this.is = is;
	this.cs = cs;
	this.cdec = cs.newDecoder();
	tempBuffer = new byte[(int)Math.ceil(cdec.maxCharsPerByte())];
    }
    
    public void close() throws IOException {}
    public int read(char[] cbuf, int off, int len) throws IOException {
	int curByte;
	int charsRead = 0;
	
	while(charsRead < len) {
	    while(true) {
		curByte = is.read();
		if(curByte >= 0) {
		    tempBuffer[tempBufferPtr++] = (byte)curByte;
		    ByteBuffer bb = ByteBuffer.wrap(tempBuffer, 0, tempBufferPtr);
		    CharBuffer out = CharBuffer.allocate(1);
		    
		    CoderResult res = cdec.decode(bb, out, true);
		    if(!res.isError()) {
			cbuf[off+charsRead] = out.get(0);//cab.put(out.get(0));
			break;
		    }
		    else if(tempBufferPtr == tempBuffer.length) {
			System.err.println(res.toString());
			throw new RuntimeException("error while decoding");
		    }
		}
		else
		    return charsRead;
	    }
	    ++charsRead;
	    tempBufferPtr = 0;
	}

	return charsRead;
    }
}