package org.catacombae.io;
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
    public int available() throws IOException { return is.available(); }

    /** @see java.io.InputStream */
    public void close() throws IOException { is.close(); }

    /** @see java.io.InputStream */
    public void mark(int readLimit) { throw new RuntimeException("Mark/reset not supported"); }
    
    /** @see java.io.InputStream */
    public boolean markSupported() { return false; }

    /** @see java.io.InputStream */
    public int read() throws IOException {
	//System.out.println("read();");
	int res = is.read();
	if(res > 0)
	    ++bytePos;
	return res;
    }

    /** @see java.io.InputStream */
    public int read(byte[] b) throws IOException {
	//System.out.println("read(b.length=" + b.length + ");");
	int res = is.read(b);
	if(res > 0) bytePos += res;
	return res;
    }

    /** @see java.io.InputStream */
    public int read(byte[] b, int off, int len) throws IOException {
	//System.out.println("read(b.length=" + b.length + ", " + off + ", " + len + ");");
	int res = is.read(b, off, len);
	if(res > 0) bytePos += res;
	return res;
    }

    /** @see java.io.InputStream */
    public void reset() throws IOException { throw new RuntimeException("Mark/reset not supported"); }

    /** @see java.io.InputStream */
    public long skip(long n) throws IOException {
	System.out.println("skip(" + n + ");");
	long res = is.skip(n);
	if(res > 0) bytePos += res;
	return res;
    }
}