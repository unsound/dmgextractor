package org.catacombae.xml.parser;

import java.io.*;
import java.nio.*;

public class MutableInputStreamReader extends Reader {
    private InputStream iStream;
    private InputStreamReader isReader;
    
    public MutableInputStreamReader(InputStream iStream, String charset) throws UnsupportedEncodingException {
	this.iStream = iStream;
	this.isReader = new InputStreamReader(iStream, charset);
    }
    public void close() throws IOException { isReader.close(); }
    public void mark(int readAheadLimit) throws IOException { isReader.mark(readAheadLimit); }
    public boolean markSupported() { return isReader.markSupported(); }
    public int read() throws IOException { return isReader.read(); }
    public int read(char[] cbuf) throws IOException { return isReader.read(cbuf); }
    public int read(char[] cbuf, int off, int len) throws IOException { return isReader.read(cbuf, off, len); }
    public int read(CharBuffer target) throws IOException { return isReader.read(target); }
    public boolean ready() throws IOException { return isReader.ready(); }
    public void reset() throws IOException { isReader.reset(); }
    public long skip(long n) throws IOException { return isReader.skip(n); }

    public void changeEncoding(String charset) throws UnsupportedEncodingException {
	isReader = new InputStreamReader(iStream, charset);
    }
}