/*-
 * Copyright (C) 2007 Erik Larsson
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