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