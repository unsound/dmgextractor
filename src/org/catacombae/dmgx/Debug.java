package org.catacombae.dmgx;

public class Debug {
    public static boolean debug = true;
    
    public static void warning(String message) {
	if(debug)
	    System.err.println(message);
    }
    
    public static void notification(String message) {
	if(debug)
	    System.out.println("------->NOTE: " + message);
    }
}
