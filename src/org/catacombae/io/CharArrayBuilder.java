package org.catacombae.io;
/* unused... remnants of old ideas */
public class CharArrayBuilder {
    private int growConstant;
    private char[] backingArray;
    private int pos;
    
    public CharArrayBuilder() { this(512); }
    public CharArrayBuilder(int capacity) {
	growConstant = capacity;
	backingArray = new char[capacity];
	pos = 0;
    }
    
    public void put(char c) {
	if(pos == backingArray.length)
	    growArray();
	backingArray[pos++] = c;
    }
    
    public byte[] clearBuffer() {
	byte[] result = new byte[pos];
	System.arraycopy(backingArray, 0, result, 0, result.length);
	backingArray = new byte[growConstant];
	pos = 0;
	return result;
    }
    
    private void growArray() {
	byte[] oldArray = backingArray;
	byte[] newArray = new byte[backingArray.length+growConstant];
	System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
	backingArray = newArray;
    }
}