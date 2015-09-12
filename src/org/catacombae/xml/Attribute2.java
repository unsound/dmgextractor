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

public class Attribute2 {
    public final String localName;
    public final String qName;
    public final String type;
    public final String URI;
    public final String value;
    public Attribute2(String localName, String qName, String type, String URI, String value) {
	this.localName = localName;
	this.qName = qName;
	this.type = type;
	this.URI = URI;
	this.value = value;
    }
}
