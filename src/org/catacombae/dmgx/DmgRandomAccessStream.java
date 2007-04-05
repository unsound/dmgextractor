package org.catacombae.dmgx;
import org.catacombae.io.*;
import java.io.*;

public class DmgRandomAccessStream implements RandomAccessStream {
    /*
      We have a string of data divided into blocks. Different algorithms must be applied to
      different types of blocks in order to extract the data.
      Whe
     */
    private DmgFile dmgFile;
    private DMGBlock[] allBlocks;
    private DMGBlock currentBlock;
    private int currentBlockIndex;
    private DMGBlockInputStream currentBlockStream;
    
    private long length;
    private long globalFilePointer = 0;
    private boolean seekCalled = false;

    public DmgRandomAccessStream(DmgFile dmgFile) throws IOException {
	this.dmgFile = dmgFile;
	Plist plist = dmgFile.getView().getPlist();
	DmgPlistPartition[] partitions = plist.getPartitions();

	int totalBlockCount = 0;
	for(DmgPlistPartition pp : partitions)
	    totalBlockCount += pp.getBlockCount();
	
	allBlocks = new DMGBlock[totalBlockCount];
	int pos = 0;
	for(DmgPlistPartition pp : partitions) {
	    DMGBlock[] blocks = pp.getBlocks();
	    System.arraycopy(blocks, 0, allBlocks, pos, blocks.length);
	    pos += blocks.length;
	    length += pp.getPartitionSize();
	}
	if(totalBlockCount > 0) {
	    currentBlock = allBlocks[0];
	    currentBlockIndex = 0;
	    repositionStream();
	}
	else
	    throw new RuntimeException("Could not find any blocks in the DMG file...");
    }
    
    /** @see java.io.RandomAccessFile */
    public void close() throws IOException {}

    /** @see java.io.RandomAccessFile */
    public long getFilePointer() throws IOException { return globalFilePointer; }

    /** @see java.io.RandomAccessFile */
    public long length() throws IOException { return length; }

    /** @see java.io.RandomAccessFile */
    public int read() throws IOException {
	byte[] b = new byte[1];
	return read(b, 0, 1);
    }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b) throws IOException { return read(b, 0, b.length); }

    /** @see java.io.RandomAccessFile */
    public int read(byte[] b, int off, int len) throws IOException {
 	//System.out.println("DmgRandomAccessStream.read(b, " + off + ", " + len + ") {");
	if(seekCalled) {
	    seekCalled = false;
	    //System.out.println("  Repositioning stream after seek (global file pointer: " + globalFilePointer + ")...");
	    try { repositionStream(); }
	    catch(RuntimeException re) {
// 		System.out.println("return: -1 }");
		return -1;
	    }
	}
	int bytesRead = 0;
	while(bytesRead < len) {
	    
	    int curBytesRead = currentBlockStream.read(b, off+bytesRead, len-bytesRead);
	    if(curBytesRead < 0) {
		//System.out.println("  Repositioning stream...");
		try { repositionStream(); }
		catch(RuntimeException re) {
		    if(bytesRead == 0)
			bytesRead = -1; // If no bytes could be read, we must indicate that the stream has no more data
		    break;
		}
		curBytesRead = currentBlockStream.read(b, off+bytesRead, len-bytesRead);
		if(curBytesRead < 0) {
		    throw new RuntimeException("No bytes could be read, and no exception was thrown! Program error...");
// 		    if(bytesRead == 0)
// 			bytesRead = -1; // If no bytes could be read, we must indicate that the stream has no more data
// 		    break;
		}
	    }
	    
// 	    if(curBytesRead >= 0)
	    bytesRead += curBytesRead;
	    globalFilePointer += curBytesRead;
	}
	
	
	    
// 	System.out.println("return: " + bytesRead + " }");
	return bytesRead;
    }

    /** @see java.io.RandomAccessFile */
    public void seek(long pos) throws IOException {
	seekCalled = true;
	globalFilePointer = pos;
    }
    
    private void repositionStream() throws IOException {
	// if the global file pointer is not within the bounds of the current block, then find the accurate block
	if(!(currentBlock.getTrueOutOffset() <= globalFilePointer &&
	     (currentBlock.getTrueOutOffset()+currentBlock.getOutSize()) > globalFilePointer)) {
	    DMGBlock soughtBlock = null;
	    for(DMGBlock dblk : allBlocks) {
		long startPos = dblk.getTrueOutOffset();
		long endPos = startPos + dblk.getOutSize();
		if(startPos <= globalFilePointer && endPos > globalFilePointer) {
		    soughtBlock = dblk;
		    break;
		}
	    }
	    if(soughtBlock != null) {
		//System.out.println("REPOSITION " + currentBlock.getBlockTypeAsString() + "(" + currentBlock.getTrueOutOffset() + "," + currentBlock.getOutSize() + ") -> " + soughtBlock.getBlockTypeAsString() + "(" + soughtBlock.getTrueOutOffset() + "," + soughtBlock.getOutSize() + ")");
// 		if(soughtBlock.getTrueOutOffset() == currentBlock.getTrueOutOffset()+currentBlock.getOutSize())
// 		    System.out.println("  Continuous! :)");
// 		else
// 		    System.out.println("  FUCKADSFOA!!!1one");
		currentBlock = soughtBlock;
	    }
	    else
		throw new RuntimeException("Trying to seek outside bounds.");
	}
	
	currentBlockStream = DMGBlockInputStream.getStream(dmgFile.getStream(), currentBlock);
	long bytesToSkip = globalFilePointer - currentBlock.getTrueOutOffset();
	currentBlockStream.skip(bytesToSkip);
    }
    
    public static void main(String[] args) throws IOException {
	System.out.println("DMGRandomAccessStream simple test program");
	System.out.println("(Simply extracts the contents of a DMG file to a designated output file)");
	if(args.length != 2)
	    System.out.println("  ERROR: You must supply exactly two arguments: 1. the DMG, 2. the output file");
	else {
	    byte[] buffer = new byte[4096];
	    DmgRandomAccessStream dras = 
		new DmgRandomAccessStream(new DmgFile(new RandomAccessFileStream(new RandomAccessFile(args[0], "r"))));
	    FileOutputStream fos = new FileOutputStream(args[1]);
	    
	    long totalBytesRead = 0;
	    
	    int bytesRead = dras.read(buffer);
	    while(bytesRead > 0) {
		totalBytesRead += bytesRead;
		fos.write(buffer, 0, bytesRead);
		bytesRead = dras.read(buffer);
	    }
	    System.out.println("Done! Extracted " + totalBytesRead + " bytes.");
	    System.out.println("Length: " + dras.length() + " bytes");
	}
    }
}
