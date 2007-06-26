package org.catacombae.dmgx;
import java.util.*;
import java.io.*;

public class DmgPlistPartition {
    private String name;
    private String id;
    private String attributes;
    private DMGBlock[] blockList;
    private long partitionSize;
    
    // Incoming variables
    private final long previousOutOffset;
    private final long previousInOffset;
    
    // Outgoing variables
    private long finalOutOffset = -1;
    private long finalInOffset = -1;
    
    private class BlockIterator implements Iterator<DMGBlock> {
	private DMGBlock[] blocks;
	private int pointer, endOffset;
	public BlockIterator(DMGBlock[] blocks) { this(blocks, 0, blocks.length); }
	public BlockIterator(DMGBlock[] blocks, int offset, int length) {
	    this.blocks = blocks;
	    this.pointer = offset;
	    this.endOffset = offset+length;
	}
	public boolean hasNext() { return pointer < endOffset; }
	public DMGBlock next() { return blocks[pointer++]; }
	public void remove() { throw new UnsupportedOperationException(); }
    }
    
    public DmgPlistPartition(String name, String id, String attributes, byte[] data, 
			     long previousOutOffset, long previousInOffset) throws IOException {
	this(name, id, attributes, new ByteArrayInputStream(data), previousOutOffset, previousInOffset);
    }
    public DmgPlistPartition(String name, String id, String attributes, InputStream data, 
			     long previousOutOffset, long previousInOffset) throws IOException {
	this.name = name;
	this.id = id;
	this.attributes = attributes;
	this.previousOutOffset = previousOutOffset;
	this.previousInOffset = previousInOffset;

	this.blockList = parseBlocks(data);
	this.partitionSize = calculatePartitionSize(blockList);
    }
    
    public String getName() {
	return name;
    }
    
    public String getID() {
	return id;
    }

    public String getAttributes() {
	return attributes;
    }
    
    public long getPartitionSize() {
	return partitionSize;
    }
    
    /** Copies all blocks to a newly allocated array. Might waste some memory. */
    public DMGBlock[] getBlocks() {
	DMGBlock[] res = new DMGBlock[blockList.length];
	for(int i = 0; i < res.length; ++i)
	    res[i] = blockList[i];
	return res;
    }
    /** Returns an iterator over all the DMGBlocks that describe the contents of this partition. */
    public Iterator<DMGBlock> getBlockIterator() {
	return new BlockIterator(blockList);
    }
    
    public int getBlockCount() {
	return blockList.length;
    }

    public long getFinalOutOffset() {
	if(finalOutOffset < 0)
	    throw new RuntimeException("parseBlocks has not yet been called!");
	return finalOutOffset;
    }
    public long getFinalInOffset() {
	if(finalInOffset < 0)
	    throw new RuntimeException("parseBlocks has not yet been called!");
	return finalInOffset;
    }
    
    private DMGBlock[] parseBlocks(InputStream is) throws IOException {
	//System.err.println("<DmgPlistPartition.parseBlocks>");
	//byte[] ccTemp = new byte[0xCC];
	long bytesSkipped = is.read(new byte[0xCC]);//is.read(ccTemp);//is.skip(0xCC);//int offset = 0xCC;
	//ccTemp = null; // not needed
	//System.err.println("skipped " + bytesSkipped + " bytes");
	if(bytesSkipped != 0xCC)
	    throw new RuntimeException("Could not skip the desired amount of bytes...");
	
	int blockNumber = 0; // Increments by one for each block we read (each iteration in the while loop below)
	
	/* These two variables are part of the "hack" described below. */
	long lastByteReadInBlock = -1;
	boolean addInOffset = false;
	
	byte[] blockData = new byte[DMGBlock.structSize()];
	
	LinkedList<DMGBlock> blocks = new LinkedList<DMGBlock>();
	while(true) { //offset <= data.length-DMGBlock) {
	    int bytesRead = is.read(blockData);
	    //System.err.println("Looping (read " + bytesRead + " bytes)");
	    if(bytesRead == -1)
		break;
	    else if(bytesRead != blockData.length)
		throw new RuntimeException("Could not read the desired amount of bytes... (desired: " + blockData.length + " read: " + bytesRead + ")");
	    //DMGBlock currentBlock = new DMGBlock(blockData, 0, previousOutOffset);
	    long inOffset = DMGBlock.peekInOffset(blockData, 0);
	    long inSize = DMGBlock.peekInOffset(blockData, 0);
	    //System.err.println("  blockType=" + currentBlock.getBlockTypeAsString());
	    //(new BufferedReader(new InputStreamReader(System.in))).readLine();

	    // Set compensation to the end of the output data of the previous partition to get true offset in outfile.
	    //currentBlock.setOutOffsetCompensation(previousOutOffset);
	    long outOffsetCompensation = previousOutOffset;
	    
	    // Update pointer to the last byte read in the last block
	    if(lastByteReadInBlock == -1)
		lastByteReadInBlock = inOffset;
	    lastByteReadInBlock += inSize;
	    
	    /* The lines below are a "hack" that I had to do to make dmgx work with
	       certain dmg-files. I don't understand the issue at all, which is why
	       this hack is here, but sometimes inOffset == 0 means that it is 0
	       relative to the previous partition's last inOffset. And sometimes it
	       doesn't (meaning the actual position 0 in the dmg file). */
	    if(inOffset == 0 && blockNumber == 0) {
		Debug.notification("Detected inOffset == 0, setting addInOffset flag.");
		addInOffset = true;
	    }
	    long inOffsetCompensation = 0;
	    if(addInOffset) {
		Debug.notification("addInOffset mode: inOffset tranformation " + inOffset + "->" + 
				   (inOffset+previousInOffset));
		inOffsetCompensation = previousInOffset;
	    }
	    
	    DMGBlock currentBlock = new DMGBlock(blockData, 0, outOffsetCompensation, inOffsetCompensation);
	    blocks.add(currentBlock);
	    //offset += 40;
	    ++blockNumber;
	    
	    //System.out.println("  " + currentBlock.toString());
	    
	    // Return if we have reached the end, and update
	    if(currentBlock.getBlockType() == DMGBlock.BT_END) {
		finalOutOffset = currentBlock.getTrueOutOffset();
		finalInOffset = previousInOffset + lastByteReadInBlock;
		
		if(is.read() != -1)
		    Debug.warning("Encountered additional data in blkx blob.");
		return blocks.toArray(new DMGBlock[blocks.size()]);
	    }
	}
	
	throw new RuntimeException("No BT_END block found!");
    }
        
    public static long calculatePartitionSize(DMGBlock[] data) throws IOException {
	long partitionSize = 0;

	for(DMGBlock db : data)
	    partitionSize += db.getOutSize();
	
	return partitionSize;
    }
}
