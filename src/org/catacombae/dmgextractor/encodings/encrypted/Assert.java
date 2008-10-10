/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmgextractor.encodings.encrypted;

/**
 *
 * @author erik
 */
public class Assert {
    public static void eq(long a, long b) {
        eq(a, b, null);
    }
    public static void eq(long a, long b, String message) {
        if(a != b)
            throw new InvalidAssertionException("Equality asserion " + a +
                    " == " + b + " failed!" +
                    (message!=null ? " Message: "+message : ""));
    }
    public static void neq(long a, long b) {
        neq(a, b, null);
    }
    public static void neq(long a, long b, String message) {
        if(a == b)
            throw new InvalidAssertionException("Non-equality asserion " + a +
                    " != " + b + " failed!" +
                    (message!=null ? " Message: "+message : ""));
    }

    public static class InvalidAssertionException extends RuntimeException {
        public InvalidAssertionException(String message) {
            super(message);
        }
    }
}
