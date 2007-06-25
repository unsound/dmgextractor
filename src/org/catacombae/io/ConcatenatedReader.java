package org.catacombae.io;
import java.io.*;

public class ConcatenatedReader extends Reader {
    private final Reader[] sources;
    private int currentSource;
    private long charPos = 0;
    public ConcatenatedReader(Reader[] sources) {
	this.sources = sources;
	this.currentSource = 0;
    }
    public void close() throws IOException {
	for(Reader r : sources)
	    r.close();
    }
    public int read(char[] cbuf, int off, int len) throws IOException {
	int bytesRead = 0;
	while(bytesRead < len) {
	    Reader currentReader = sources[currentSource];
	    int currentRead = currentReader.read(cbuf, off+bytesRead, len-bytesRead);
	    while(currentRead != -1 && bytesRead < len) {
		bytesRead += currentRead;
		currentRead = currentReader.read(cbuf, off+bytesRead, len-bytesRead);
	    }
	    if(currentRead == -1) {
		if(currentSource+1 < sources.length)
		    ++currentSource;
		else
		    break; // There wasn't enough data
	    }
	}
	return bytesRead;
    }
}