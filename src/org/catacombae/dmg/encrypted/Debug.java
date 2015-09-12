/*-
 * Copyright (C) 2008 Erik Larsson
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

package org.catacombae.dmg.encrypted;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class Debug {
    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {}
        
    }
    private static boolean debugEnabled = false;
    public static final PrintStream ps;
    
    static {
        if(debugEnabled)
            ps = System.err;
        else
            ps = new PrintStream(new NullOutputStream());
    }

    public static void print(String s) {
        if(debugEnabled)
            ps.println(s);
    }
}
