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
public class Test {
    public static void main(String[] args) {
        SparseBundle sb = new SparseBundle(new File(args[0]));
        System.out.println("image size: " + sb.getSize() + " bytes");
        System.out.println("band size: " + sb.getBandSize() + " bytes");
        System.out.println("band count: " + sb.getBandCount() + " bands");

        ReadableSparseBundleStream stream = new ReadableSparseBundleStream(sb);
        byte[] buf = new byte[91673];
        long bytesRead = 0;
        final long startTime = System.currentTimeMillis();
        long lastTime = startTime;

        while(true) {
            int curBytesRead = stream.read(buf);
            if(curBytesRead == -1)
                break;
            else if(curBytesRead < 0)
                throw new RuntimeException("Wtf... curBytesRead=" +
                        curBytesRead);

            bytesRead += curBytesRead;

            final long curTime = System.currentTimeMillis();
            if(curTime - lastTime > 1000) {
                System.err.println("Transferred " + bytesRead + " bytes in " +
                        (curTime-startTime)/((double) 1000.0) + " seconds.");
                lastTime = curTime;
            }
        }

        System.err.println("Transfer complete.");

        final long curTime = System.currentTimeMillis();
        System.err.println("Transferred " + bytesRead + " bytes in " +
                (curTime-startTime)/((double) 1000.0) + " seconds.");

    }
}
