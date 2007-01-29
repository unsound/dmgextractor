import org.catacombae.io.RandomAccessStream;

public class DmgRandomAccessStream implements RandomAccessStream {
    public DmgRandomAccessStream() {
	
    }
    
    /** @see java.io.RandomAccessFile */
    public void close() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException {}

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException {}

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException {}
    
}