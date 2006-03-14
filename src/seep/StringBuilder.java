/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package java.lang;

/**
 * Stupid replacement for Java 1.5's StringBuilder class, allowing
 * 1.5-applications to be recompiled with gcj.
 * String concatenation operations will be a bit slow.
 */

class StringBuilder implements java.io.Serializable, CharSequence {
    private final StringBuffer contents;

    public StringBuilder() {
	contents = new StringBuffer();
    }
    public StringBuilder(int capacity) {
	contents = new StringBuffer(capacity);
    }
//     public StringBuilder(CharSequence seq) {
// 	contents = new StringBuffer(seq);
//     }
    public StringBuilder(String str) {
	contents = new StringBuffer();
    }
    private StringBuilder(StringBuffer contents) {
	this.contents = contents;
    }
    public StringBuilder append(boolean bool) {
	contents.append(bool);
	return this;
    }
    public StringBuilder append(char ch) {
	contents.append(ch);
	return this;
    }
    public StringBuilder append(char[] data) {
	contents.append(data);
	return this;
    }
    public StringBuilder append(char[] data, int offset, int count) {
	contents.append(data, offset, count);
	return this;
    }
    public StringBuilder append(double dnum) {
	contents.append(dnum);
	return this;
    }
    public StringBuilder append(float fnum) {
	contents.append(fnum);
	return this;
    }
    public StringBuilder append(int inum) {
	contents.append(inum);
	return this;
    }
//     public StringBuilder append(CharSequence seq) {
// 	contents.append(seq);
// 	return this;
//     }
//     public StringBuilder append(CharSequence seq, int start, int end) {
// 	contents.append(seq, start, end);
// 	return this;
//     }
    public StringBuilder append(Object obj) {
	contents.append(obj);
	return this;
    }
    public StringBuilder append(String str) {
	contents.append(str);
	return this;
    }
    public StringBuilder append(StringBuffer stringBuffer) {
	contents.append(stringBuffer);
	return this;
    }
    public StringBuilder append(long lnum) {
	contents.append(lnum);
	return this;
    }
//     public StringBuilder appendCodePoint(int code) {
// 	contents.appendCodePoint(code);
// 	return this;
//     }
    public int capacity() {
	return contents.capacity();
    }
    public char charAt(int index) {
	return contents.charAt(index);
    }
    public StringBuilder delete(int start, int end) {
	contents.delete(start, end);
	return this;
    }
    public StringBuilder deleteCharAt(int index) {
	contents.deleteCharAt(index);
	return this;
    }
    public void ensureCapacity(int minimumCapacity) {
	contents.ensureCapacity(minimumCapacity);
    }
    public void getChars(int srcOffset, int srcEnd, char[] dst, int dstOffset) {
	contents.getChars(srcOffset, srcEnd, dst, dstOffset);
    }
    public int indexOf(String str) {
	return contents.indexOf(str);
    }
    public int indexOf(String str, int fromIndex) {
	return contents.indexOf(str, fromIndex);
    }
    public StringBuilder insert(int offset, boolean bool) {
	contents.insert(offset, bool);
	return this;
    }
    public StringBuilder insert(int offset, char ch) {
	contents.insert(offset, ch);
	return this;
    }
    public StringBuilder insert(int offset, char[] data) {
	contents.insert(offset, data);
	return this;
    }
    public StringBuilder insert(int offset, char[] str, int str_offset, int len) {
	contents.insert(offset, str, str_offset, len);
	return this;
    }
    public StringBuilder insert(int offset, double dnum) {
	contents.insert(offset, dnum);
	return this;
    }
    public StringBuilder insert(int offset, float fnum) {
	contents.insert(offset, fnum);
	return this;
    }
    public StringBuilder insert(int offset, int inum) {
	contents.insert(offset, inum);
	return this;
    }
//     public StringBuilder insert(int offset, CharSequence sequence) {
// 	contents.insert(offset, sequence);
// 	return this;
//     }
//     public StringBuilder insert(int offset, CharSequence sequence, int start, int end) {
// 	contents.insert(offset, sequence, start, end);
// 	return this;
//     }
    public StringBuilder insert(int offset, Object obj) {
	contents.insert(offset, obj);
	return this;
    }
    public StringBuilder insert(int offset, String str) {
	contents.insert(offset, str);
	return this;
    }
    public StringBuilder insert(int offset, long lnum) {
	contents.insert(offset, lnum);
	return this;
    }
    public int lastIndexOf(String str) {
	return contents.lastIndexOf(str);
    }
    public int lastIndexOf(String str, int fromIndex) {
	return contents.lastIndexOf(str, fromIndex);
    }
    public int length() {
	return contents.length();
    }
    public StringBuilder replace(int start, int end, String str) {
	contents.replace(start, end, str);
	return this;
    }
    public StringBuilder reverse() {
	contents.reverse();
	return this;
    }
    public void setCharAt(int index, char ch) {
	contents.setCharAt(index, ch);
    }
    public void setLength(int newLength) {
	contents.setLength(newLength);
    }
    public CharSequence subSequence(int beginIndex, int endIndex) {
	return contents.subSequence(beginIndex, endIndex);
    }
    public String substring(int beginIndex) {
	return contents.substring(beginIndex);
    }
    public String substring(int beginIndex, int endIndex) {
	return contents.substring(beginIndex, endIndex);
    }
    public String toString() {
	return contents.toString();
    }
}
