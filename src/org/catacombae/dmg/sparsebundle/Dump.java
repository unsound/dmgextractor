/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.File;

/**
 *
 * @author erik
 */
public class Dump {
    public static void main(String[] args) {
        ReadableSparseBundleStream stream =
                new ReadableSparseBundleStream(new File(args[0]));
        byte[] buf = new byte[512*1024];
        long bytesRead = 0;

        while(true) {
            int curBytesRead = stream.read(buf);
            if(curBytesRead == -1)
                break;
            else if(curBytesRead < 0)
                throw new RuntimeException("Wtf... curBytesRead=" +
                        curBytesRead);

            bytesRead += curBytesRead;

            System.out.write(buf, 0, curBytesRead);
        }
    }
}
