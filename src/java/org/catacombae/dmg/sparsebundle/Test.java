/*-
 * Copyright (C) 2011 Erik Larsson
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

package org.catacombae.dmg.sparsebundle;

import java.io.File;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
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
