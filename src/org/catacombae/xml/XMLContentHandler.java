/*-
 * Copyright (C) 2007 Erik Larsson
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

import java.util.List;
import java.nio.charset.Charset;

public abstract class XMLContentHandler {
    protected Charset encoding;
    public XMLContentHandler(Charset encoding) {
	this.encoding = encoding;
    }
    //public void doctype(String
    public abstract void xmlDecl(String version, String encoding, Boolean standalone);
    public abstract void pi(String id, String content);
    public abstract void comment(String comment);
    public abstract void doctype(String name, ExternalID eid); // Needs a DTD description also
    public abstract void cdata(String cdata);
    public abstract void emptyElement(String name, List<Attribute> attributes);
    public abstract void startElement(String name, List<Attribute> attributes);
    public abstract void endElement(String name);
    public abstract void chardata(int beginLine, int beginColumn, int endLine, int endColumn);
    public abstract void reference(String ref);
}