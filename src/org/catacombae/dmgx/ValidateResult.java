package org.catacombae.dmgx;

import java.util.LinkedList;

class ValidateResult {
    private final LinkedList<String> errors = new LinkedList<String>();
    private final LinkedList<String> warnings = new LinkedList<String>();
    
    public ValidateResult() {
    }
    
    public void addError(String message) {
	errors.addLast(message);
    }
    public void addWarning(String message) {
	warnings.addLast(message);
    }
    
    public String[] getErrors() {
	return errors.toArray(new String[errors.size()]);
    }
    public String[] getWarnings() {
	return warnings.toArray(new String[warnings.size()]);
    }
}