/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmgextractor.encodings.encrypted;

import java.io.PrintStream;

/**
 *
 * @author erik
 */
class Debug {
    private static boolean debugEnabled = true;
    public static final PrintStream ps = System.err;

    public static void print(String s) {
        if(debugEnabled)
            ps.println(s);
    }
}
