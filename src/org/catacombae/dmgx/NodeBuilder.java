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

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

/**
 * Plugs into a SAXParser to build a tree of XMLElements representing the document.
 */
class NodeBuilder extends DefaultHandler {
    /** NEVER try to obtain anything from this except its children. */
    private XMLNode artificialRoot;

    private XMLNode currentNode;
    
    public NodeBuilder() {
	artificialRoot = new XMLNode(null, null, null, null, null);
	currentNode = artificialRoot;
    }
    public void startElement(String namespaceURI, String sName, String qName,
			     Attributes attrs) throws SAXException {
	//System.out.println("SE");
	Attribute[] attributes = new Attribute[attrs.getLength()];
	for(int i = 0; i < attributes.length; ++i) {
	    attributes[i] = new Attribute(attrs.getLocalName(i), attrs.getQName(i), 
					  attrs.getType(i), attrs.getURI(i), attrs.getValue(i));
	}
	
	XMLNode newNode = new XMLNode(namespaceURI, sName, qName,
				      attributes, currentNode);
	currentNode.addChild(newNode);
	currentNode = newNode;
    }
    public void endElement(String namespaceURI, String sName,
			   String qName) throws SAXException {
	//System.out.println("EE");
	currentNode = currentNode.parent;
    }
    public void characters(char[] buf, int offset, int len)
        throws SAXException {
	//System.out.println("CH");
	String s = new String(buf, offset, len).trim();
	if(s.length() != 0)
	    currentNode.addChild(new XMLText(s));
    }
    public void notationDecl(String name, String publicId,
			     String systemId) throws SAXException {
	System.out.println("notationDecl(" + name + ", " + 
			   publicId + ", " + systemId + ");");
    }
    public XMLNode[] getRoots() throws RuntimeException {
	if(artificialRoot != currentNode)
	    throw new RuntimeException("Tree was not closed!");
	
	int numberOfNodes = 0;
	for(XMLElement xe : artificialRoot.getChildren()) {
	    if(xe instanceof XMLNode)
		++numberOfNodes;
	}
	XMLNode[] result = new XMLNode[numberOfNodes];
	int i = 0;
	for(XMLElement xe : artificialRoot.getChildren()) {
	    if(xe instanceof XMLNode)
		result[i++] = (XMLNode)xe;
	}
	return result;
    }
}
