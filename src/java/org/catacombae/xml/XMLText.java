/*-
 * Copyright (C) 2006 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.xml;

import org.catacombae.dmgextractor.io.*;
import java.io.*;
import java.nio.charset.Charset;

public class XMLText extends XMLElement {
    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final char TAB = '\t';
    private final String text;
    private int beginLine = -1, beginColumn = -1, endLine = -1, endColumn = -1;
    private long beginOffset, endOffset;
    private final SynchronizedRandomAccessStream xmlFile;
    private Charset encoding;
    
    public XMLText(String text) {
	this.text = text;
	this.xmlFile = null;
    }
    public XMLText(SynchronizedRandomAccessStream xmlFile, Charset encoding,
		   long beginOffset, long endOffset) {
	this.text = null;
	this.xmlFile = xmlFile;
	this.encoding = encoding;
	this.beginOffset = beginOffset;
	this.endOffset = endOffset;
	//SynchronizedRandomAccessStream sras = new SynchronizedRandomAccessStream(new RandomAccessFileStream(raf));
    }

    /** This way of dealing with the issue of lines and columns is very heavy and fragile as it tries to
	read through the entire xmlFile to seek forward to the desired lines and columns (there's no
	algorithmic way to do this, so we have toseek exhaustively). The calculation will not be performed
	in the constructor, but on the first call to getText()... (in order to reduce workload) */
    public XMLText(SynchronizedRandomAccessStream xmlFile, Charset encoding,
		   int beginLine, int beginColumn, int endLine, int endColumn) {
	this(xmlFile, encoding, -1, -1); // we set the -1 fields later, ...
	if(endLine < beginLine || (endLine == beginLine && endColumn < beginColumn))
	    throw new IllegalArgumentException("negative interval length");
	
	this.beginLine = beginLine;
	this.beginColumn = beginColumn;
	this.endLine = endLine;
	this.endColumn = endColumn;
    }
    
    public Reader getText() throws IOException {
	if(text == null) {
	    if(beginOffset == -1 && endOffset == -1)
		calculateOffsets();
	    return new InputStreamReader(new RandomAccessInputStream(xmlFile, beginOffset, endOffset-beginOffset), encoding);
	}
	else
	    return new StringReader(text);
    }
    
    private void calculateOffsets() throws IOException {
	ByteCountInputStream bcis = new ByteCountInputStream(new BufferedInputStream(new RandomAccessInputStream(xmlFile)));
	Reader lnr = new CharByCharReader(bcis, encoding);
	//Vi har xmlFile
	//CharsetDecoder decoder = Charset.newDecoder();
	
	boolean previousCR = false;
	long lineNumber = 1, colNumber = -1;
	//int beginOffset = -1, endOffset = -1;
	
	int currentChar = 0;
	while(currentChar >= 0) {
	    char c = (char)currentChar;
	    
	    boolean lfskip = false;
	    if(c == CR) {
		++lineNumber;
		previousCR = true;
	    }
	    else if(c == LF) {
		if(!previousCR) {
		    ++lineNumber;
		    colNumber = 0;
		}
		else {
		    previousCR = false;
		    lfskip = true; // We haven't changed the col or line number in this iteration, as in the other cases
		}
	    }
	    else if(c == TAB) {
		colNumber += 8;
		previousCR = false;
	    }
	    else {
		++colNumber;
		previousCR = false;
	    }
	    
// 	    System.err.println("Trying to read... lineNumber=" + lineNumber + " colNumber=" + colNumber);
	    
	    if(!lfskip) {
		if(lineNumber == beginLine && colNumber == beginColumn)
		    beginOffset = bcis.getBytesRead()-1; // We have already passed the position.
		if(lineNumber == endLine && colNumber == endColumn) {
		    endOffset = bcis.getBytesRead();
		    break;
		}
	    }
	    currentChar = lnr.read();
	}
	
	if(beginOffset == -1 || endOffset == -1)
	    throw new RuntimeException("Could not find the requested interval! (begin: (" + beginLine + "," + beginColumn + ") end: (" + endLine + "," + endColumn + "))");
// 	else
// 	    System.out.println("Terminating with beginOffset=" + beginOffset + " endOffset=" + endOffset);
    }
        
    protected void _printTree(PrintStream pw, int level) {
	for(int i = 0; i < level; ++i)
	    pw.print(" ");
	pw.println(text.toString());
    }

//     public static void main(String[] args) {
// 	System.out.println(args[0] + " " + args[1]);
//     }
}
