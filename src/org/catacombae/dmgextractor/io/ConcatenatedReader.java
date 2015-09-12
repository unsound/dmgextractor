/*-
 * Copyright (C) 2007 Erik Larsson
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

package org.catacombae.dmgextractor.io;
import java.io.*;

public class ConcatenatedReader extends Reader {
    private final Reader[] sources;
    private int currentSource;
    private long charPos = 0;
    public ConcatenatedReader(Reader[] sources) {
	this.sources = sources;
	this.currentSource = 0;
    }
    public void close() throws IOException {
	for(Reader r : sources)
	    r.close();
    }
    public int read(char[] cbuf, int off, int len) throws IOException {
	int bytesRead = 0;
	while(bytesRead < len) {
	    Reader currentReader = sources[currentSource];
	    int currentRead = currentReader.read(cbuf, off+bytesRead, len-bytesRead);
	    while(currentRead != -1 && bytesRead < len) {
		bytesRead += currentRead;
		currentRead = currentReader.read(cbuf, off+bytesRead, len-bytesRead);
	    }
	    if(currentRead == -1) {
		if(currentSource+1 < sources.length)
		    ++currentSource;
		else
		    break; // There wasn't enough data
	    }
	}
	return bytesRead;
    }
}
