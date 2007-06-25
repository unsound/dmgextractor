package org.catacombae.xml;

public class ExternalID {
    public static final int SYSTEM = 0;
    public static final int PUBLIC = 1;
    public int type;
    public String pubidLiteral;
    public String systemLiteral;

    public ExternalID(String pubidLiteral, String systemLiteral) {
	this.pubidLiteral = pubidLiteral;
	this.systemLiteral = systemLiteral;
	this.type = PUBLIC;
    }
    public ExternalID(String systemLiteral) {
	this.systemLiteral = systemLiteral;
	this.type = SYSTEM;
    }
}