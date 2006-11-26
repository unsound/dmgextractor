package org.catacombae.dmgx;

import java.util.LinkedList;
import java.io.*;

/**
 * Wrapper to allow processing of a list of dmg files. (I was too lazy
 * to write a shell script for this..)
 */
public class ValidateDmgs {
    public static void main(String[] args) throws IOException {
	LinkedList<String> fileList = new LinkedList<String>();
	for(String currentList : args) {
	    try {
		BufferedReader listIn = new BufferedReader(new InputStreamReader(new FileInputStream(new File(currentList))));
		String currentDmg = listIn.readLine();
		while(currentDmg != null) {
		    fileList.add(currentDmg);
		    currentDmg = listIn.readLine();
		}
	    } catch(IOException ioe) {
		ioe.printStackTrace();
	    }
	}
	ValidateDmg.main(fileList.toArray(new String[fileList.size()]));
    }
}