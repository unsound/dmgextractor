/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.xml;

import org.catacombae.io.*;
import org.catacombae.dmgextractor.Util;
import java.util.LinkedList;
import java.io.*;

public class XMLNode extends XMLElement {
    public final String namespaceURI;
    public final String sName;
    public final String qName;
    public final Attribute2[] attrs;
    public final XMLNode parent;
    private final LinkedList<XMLElement> children;
    
    public XMLNode(String namespaceURI, String sName, 
		   String qName, Attribute2[] attrs, XMLNode parent) {
	this.namespaceURI = namespaceURI;
	this.sName = sName;
	this.qName = qName;
	this.attrs = attrs;
	this.parent = parent;
	this.children = new LinkedList<XMLElement>();
    }
    
    public void addChild(XMLElement x) {
	children.addLast(x);
    }

    public void printTree(PrintStream pw) {
	_printTree(pw, 0);
    }

    protected void _printTree(PrintStream pw, int level) {
	for(int i = 0; i < level; ++i)
	    pw.print(" ");
	pw.print("<");
	pw.print(qName);
	for(Attribute2 a : attrs)
	    pw.print(" " + a.qName + "=" + a.value);
	pw.println(">");
	for(XMLElement xe : children)
	    xe._printTree(pw, level+1);
	
	for(int i = 0; i < level; ++i)
	    pw.print(" ");
	pw.println("</" + qName + ">");
    }
    public XMLElement[] getChildren() {
	return children.toArray(new XMLElement[children.size()]);
    }
    
    /**
     * The concept of "changing directory" in a tree is perhabs not a
     * perfect way to describe things. But this method will look up the
     * first subnode of our node that is of the type <code>type</code>
     * and return it.
     * If you have more than one of the same type, tough luck. You only
     * get the first.
     */
    public XMLNode cd(String type) {
	for(XMLElement xn : getChildren()) {
	    if(xn instanceof XMLNode && ((XMLNode)xn).qName.equals(type))
		return (XMLNode)xn;
	}
	return null;
    }
    /**
     * This is different from the <code>cd</code> method in that it
     * searches for a node of the type "key", and looks up the <code>
     * XMLText</code> within. It then compares the text with the String
     * <code>key</code>. If they match, it returns the node coming
     * after the key node. Else it continues to search. If no match is
     * found, <code>null</code> is returned.
     */
    public XMLNode cdkey(String key) {
	boolean keyFound = false;
	for(XMLElement xn : getChildren()) {
	    if(xn instanceof XMLNode) {
		if(keyFound)
		    return (XMLNode)xn;
	    
		else if(((XMLNode)xn).qName.equals("key")) {
		    for(XMLElement xn2 : ((XMLNode)xn).getChildren()) {
			try {
			    if(xn2 instanceof XMLText) {
				String s = Util.readFully(((XMLText)xn2).getText());
				//System.err.println("cdkey searching: \"" + s + "\"");
				if(s.equals(key))
				   keyFound = true;
			    }
			} catch(Exception e) { throw new RuntimeException(e); }
		    }
		}
	    }
	}
	return null;
    }
    public Reader getKeyValue(String key) {
	//System.out.println("XMLNode.getKeyValue(\"" + key + "\")");
	XMLNode keyNode = cdkey(key);
	XMLElement[] nodeChildren = keyNode.getChildren();
	if(nodeChildren.length != 1) {
	    //System.out.println("  nodeChildren.length == " + nodeChildren.length);
	    
	    LinkedList<Reader> collectedReaders = new LinkedList<Reader>();
	    for(XMLElement xe : keyNode.getChildren()) {
		if(xe instanceof XMLText) {
		    try {
			Reader xt = ((XMLText)xe).getText();
			collectedReaders.addLast(xt);
		    } catch(Exception e) { throw new RuntimeException(e); }
		    //System.out.print("\"");
		    //for(int i = 0; i < xt.length(); ++i) System.out.print(xt.charAt(i));
		    //System.out.println("\"");
		    //System.out.println("free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
		}
	    }
	    ConcatenatedReader result;
	    if(collectedReaders.size() == 0)
		result = null;
	    else {
		//System.out.println("doing a toString... free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
		//result = returnString.toString();
		//System.out.println("done.free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
		result = new ConcatenatedReader(collectedReaders.toArray(new Reader[collectedReaders.size()]));
	    }
	    return result;
	}
	else if(nodeChildren[0] instanceof XMLText) {
	    //System.err.println("Special case!");
	    try {
		return ((XMLText)nodeChildren[0]).getText();
	    } catch(Exception e) { throw new RuntimeException(e); }
	}
	else
	    return null;
    }
}
