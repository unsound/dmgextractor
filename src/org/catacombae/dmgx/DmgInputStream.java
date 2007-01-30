package org.catacombae.dmgx;
import java.io.*;

/**
 * An InputStream outputting the contents of a DMG UDIF file.
 */
public class DmgInputStream extends InputStream {
    private RandomAccessFile raf;
    private DmgFileView dmgView;
    private Plist plist;
    private DMGBlock[] allBlocks;
    private int currentBlock;
    private long totalReadableBytes;
    private long bytesRead;
    
    public DmgInputStream(RandomAccessFile raf) throws IOException {
	this.raf = raf;
	this.dmgView = new DmgFileView(raf);
	this.plist = dmgView.getPlist();
	
	DmgPlistPartition[] parts = plist.getPartitions();
	int totalNumBlocks = 0;
	for(DmgPlistPartition dpp : parts)
	    totalNumBlocks += dpp.getBlockCount();
	
	this.allBlocks = new DMGBlock[totalNumBlocks];
	int blockPtr = 0;
	for(DmgPlistPartition dpp : parts) {
	    for(DMGBlock block : dpp.getBlocks())
		allBlocks[blockPtr++] = block;
	}
	
	this.currentBlock = 0;
	
	this.totalReadableBytes = 0;
	for(DMGBlock b : allBlocks)
	    totalReadableBytes += b.getOutSize();
	
	this.bytesRead = 0;
    }
    
    public int available() throws IOException {
	long avaliable = totalReadableBytes;
	available -= bytesRead;
	if(available > Integer.MAX_INT)
	    return Integer.MAX_INT;
	else
	    return (int)available;
    }
    public void close() throws IOException {}
    public void mark(int readlimit) {}
    public boolean markSupported() {}
    public int read() throws IOException {}
    public int read(byte[] b) throws IOException {}
    public int read(byte[] b, int off, int len) throws IOException {}
    public void reset() throws IOException {}
    public long skip(long n) throws IOException {}
}
