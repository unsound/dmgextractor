/*-
 * Copyright (C) 2007-2008 Erik Larsson
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

public class Attribute {
    public static abstract class ValueComponent {
        @Override
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
        
        @Override
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