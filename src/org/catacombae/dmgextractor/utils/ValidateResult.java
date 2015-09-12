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

package org.catacombae.dmgextractor.utils;

import java.util.LinkedList;

public class ValidateResult {

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
