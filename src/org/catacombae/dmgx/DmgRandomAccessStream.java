package org.catacombae.dmgx;
import org.catacombae.io.RandomAccessStream;
import java.io.*;

public class DmgRandomAccessStream implements RandomAccessStream {
    public DmgRandomAccessStream() {
	
    }
    
    /** @see java.io.RandomAccessFile */
    public void close() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException { return -1; }

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException { return -1; }

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException { return -1; }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException { return -1; }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException { return -1; }

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException {}
    
}