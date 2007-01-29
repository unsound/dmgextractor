package org.catacombae.dmgx;

public class DmgPlistPartition {
    private String name;
    private String id;
    private String attributes;
    private DMGBlock[] data;
    public DmgPlistPartition(String name, String id, String attributes, byte[] data) {
	this.name = name;
	this.id = id;
	this.attributes = attributes;
	this.data = data;
	this.partitionSize = DMGExtractor.calculatePartitionSize(data);
    }
    
}
