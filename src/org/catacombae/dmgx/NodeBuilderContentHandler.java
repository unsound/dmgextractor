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

import org.catacombae.xml.parser.*;
import java.util.List;

public class NodeBuilderContentHandler implements XMLContentHandler {
    private NodeBuilder nodeBuilder;
    public NodeBuilderContentHandler(NodeBuilder nodeBuilder) {
	this.nodeBuilder = nodeBuilder;
    }
    public void xmlDecl(String version, String encoding, Boolean standalone) {}
    public void pi(String id, String content) {}
    public void comment(String comment) {}
    public void doctype(String name, ExternalID eid) {} // Needs a DTD description also
    public void cdata(String cdata) {
	try {
	    nodeBuilder.characters(cdata.toCharArray(), 0, cdata.length());
	} catch(Exception e) { throw new RuntimeException(e); }
    }
    public void emptyElement(String name, List<org.catacombae.xml.parser.Attribute> attributes) {
	try {
	    startElement(name, attributes);
	    endElement(name);
	} catch(Exception e) { throw new RuntimeException(e); }
    }
    public void startElement(String name, List<org.catacombae.xml.parser.Attribute> attributes) {
	try {
	    Attribute[] attrs = new Attribute[attributes.size()];
	    //for(int i = 0; i < attributes.length; ++i) {
	    int i = 0;
	    for(org.catacombae.xml.parser.Attribute a : attributes) {
		attrs[i++] = new Attribute("", a.identifier, 
					      "CDATA", "", a.value.toString());
	    }
// 	    org.xml.sax.ext.Attributes2Impl a2i = new org.xml.sax.ext.Attributes2Impl();
// 	    for(org.catacombae.xml.parser.Attribute a : attributes) {
// 		System.err.println("id: " + a.identifier + " value: " + a.value.toString());
// 		a2i.addAttribute("", a.identifier, a.identifier, "CDATA", a.value.toString());
// 	    }
	    nodeBuilder.startElementInternal(null, null, name, attrs);
	} catch(Exception e) { throw new RuntimeException(e); }	    
    }
    public void endElement(String name) {
	try {
	    nodeBuilder.endElement(null, null, name);
	} catch(Exception e) { throw new RuntimeException(e); }
    }
    public void chardata(String data) {
	try {
	    char[] ca = data.toCharArray();
	    nodeBuilder.characters(ca, 0, ca.length);
	} catch(Exception e) { throw new RuntimeException(e); }
    }
    public void reference(String ref) {
	try {
	    if(ref.startsWith("&#")) {
		// CharRef
		int[] codePoints = new int[1];
		if(ref.startsWith("&#x"))
		    codePoints[0] = Integer.parseInt(ref.substring(3), 16);
		else
		    codePoints[0] = Integer.parseInt(ref.substring(2), 10);
		char[] cp_ca = new String(codePoints, 0, 1).toCharArray();
		nodeBuilder.characters(cp_ca, 0, cp_ca.length);
	    }
	    else
		System.out.println("WARNING: Encountered external references, which cannot be resolved with this version of the parser.");
	} catch(Exception e) { throw new RuntimeException(e); }
    }
}
