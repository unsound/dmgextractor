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

import java.util.List;

public class Attribute {
    public static abstract class ValueComponent {
	public abstract String toString();
    }
    public static class StringComponent extends ValueComponent {
	public String content;
	public StringComponent(String content) { this.content = content; }
	public String toString() { return content; }
    }
    public static class ReferenceComponent extends ValueComponent {
	public String content;
	public ReferenceComponent(String content) { this.content = content; }
	public String toString() { return content; } // should resolve the reference here
    }
    public static class Value {
	public List<ValueComponent> components;
	public Value(List<ValueComponent> components) {
	    this.components = components;
	}
	public String toString() {
	    StringBuilder result = new StringBuilder();
	    for(ValueComponent vc : components)
		result.append(vc.toString());
	    return result.toString();
	}
    }
    public String identifier;
    public Value value;

    public Attribute(String identifier, Value value) {
	this.identifier = identifier;
	this.value = value;
    }
}