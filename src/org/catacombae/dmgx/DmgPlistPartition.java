package org.catacombae.dmgx;
import java.util.*;
import java.io.IOException;

public class DmgPlistPartition {
    private String name;
    private String id;
    private String attributes;
    private DMGBlock[] blockList;
    private long partitionSize;
    public DmgPlistPartition(String name, String id, String attributes, byte[] data) throws IOException {
	this.name = name;
	this.id = id;
	this.attributes = attributes;
	this.blockList = parseBlocks(data);
	this.partitionSize = DMGExtractor.calculatePartitionSize(data);
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
    
    public DMGBlock[] getBlocks() {
	DMGBlock[] res = new DMGBlock[blockList.length];
	for(int i = 0; i < res.length; ++i)
	    res[i] = blockList[i];
	return res;
    }
    
    public int getBlockCount() {
	return blockList.length;
    }
    
    private static DMGBlock[] parseBlocks(byte[] data) {
	int offset = 0xCC;
	
	LinkedList<DMGBlock> blocks = new LinkedList<DMGBlock>();
	while(offset <= data.length-40) {
	    blocks.add(new DMGBlock(data, offset));
	    offset += 40;
	}
	
	if(offset != data.length)
	    Debug.warning("Encountered additional data in blkx blob.");
	
	return blocks.toArray(new DMGBlock[blocks.size()]);
    }
}
