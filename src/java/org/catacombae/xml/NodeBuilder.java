/*-
 * Copyright (C) 2006-2008 Erik Larsson
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
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import java.nio.charset.Charset;

/**
 * Plugs into a SAXParser to build a tree of XMLElements representing the document.
 */
public class NodeBuilder extends DefaultHandler {
    /** NEVER try to obtain anything from this except its children. */
    private XMLNode artificialRoot;

    private XMLNode currentNode;
    
    public NodeBuilder() {
	artificialRoot = new XMLNode(null, null, null, null, null);
	currentNode = artificialRoot;
    }
    
    @Override
    public void startElement(String namespaceURI, String sName, String qName,
			     Attributes attrs) throws SAXException {
	//System.out.println("SE");
	Attribute2[] attributes = new Attribute2[attrs.getLength()];
	for(int i = 0; i < attributes.length; ++i) {
	    attributes[i] = new Attribute2(attrs.getLocalName(i), attrs.getQName(i), 
					   attrs.getType(i), attrs.getURI(i), attrs.getValue(i));
	}
	startElementInternal(namespaceURI, sName, qName, attributes);
    }
    
    void startElementInternal(String namespaceURI, String sName, String qName,
			      Attribute2[] attributes) throws SAXException {
	XMLNode newNode = new XMLNode(namespaceURI, sName, qName,
				      attributes, currentNode);
	currentNode.addChild(newNode);
	currentNode = newNode;
    }
    
    @Override
    public void endElement(String namespaceURI, String sName,
			   String qName) throws SAXException {
	//System.out.println("EE");
	currentNode = currentNode.parent;
    }
    
    public void characters(SynchronizedRandomAccessStream file, Charset encoding,
			   int startLine, int startColumn, int endLine, int endColumn) {
	currentNode.addChild(new XMLText(file, encoding, startLine, startColumn, endLine, endColumn));
    }
    
    @Override
    public void characters(char[] buf, int offset, int len)
        throws SAXException {
	//System.out.println("CH");
	String s = new String(buf, offset, len).trim();
	if(s.length() != 0)
	    currentNode.addChild(new XMLText(s));
    }
    
    @Override
    public void notationDecl(String name, String publicId,
			     String systemId) throws SAXException {
	//System.out.println("notationDecl(" + name + ", " + publicId + ", " + systemId + ");");
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
