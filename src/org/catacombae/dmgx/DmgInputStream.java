package org.catacombae.dmgx;
import java.io.*;

/**
 * An InputStream outputting the contents of a DMG UDIF file.
 */
public class DmgInputStream extends InputStream {
    private RandomAccessFile raf;
    private DmgFileView dmgView;
    
    public DmgInputStream(RandomAccessFile raf) {
	this.raf = raf;
	this.dmgView = new DmgFileView(raf);
    }
    
    public int available() {}
    public void close() {}
    public void mark(int readlimit) {}
    public boolean markSupported() {}
    public int read() {}
    public int read(byte[] b) {}
    public int read(byte[] b, int off, int len) {}
    public void reset() {}
    public long skip(long n) {}
}
