package org.catacombae.dmgx;

public class Debug {
    public boolean debug = true;
    
    public static void warning(String message) {
	if(debug)
	    System.out.println(message);
    }
}