/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.catacombae.dmgx;

import java.util.LinkedList;
import java.io.PrintStream;

class XMLNode extends XMLElement {
    public final String namespaceURI;
    public final String sName;
    public final String qName;
    public final Attribute[] attrs;
    public final XMLNode parent;
    private final LinkedList<XMLElement> children;
    
    public XMLNode(String namespaceURI, String sName, 
		   String qName, Attribute[] attrs, XMLNode parent) {
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
	for(Attribute a : attrs)
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
			if(xn2 instanceof XMLText && ((XMLText)xn2).text.equals(key))
			    keyFound = true;
		    }
		}
	    }
	}
	return null;
    }
    public String getKeyValue(String key) {
	XMLNode keyNode = cdkey(key);
	StringBuilder returnString = new StringBuilder();
	for(XMLElement xe : keyNode.getChildren()) {
	    if(xe instanceof XMLText)
		returnString.append(((XMLText)xe).text);
	}
	if(returnString.length() == 0)
	    return null;
	else
	    return returnString.toString();
    }
}
