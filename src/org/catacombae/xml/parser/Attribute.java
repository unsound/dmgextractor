package org.catacombae.xml.parser;

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