package org.catacombae.dmgx;
import java.util.*;

public class ConcatenatedIterator<E> implements Iterator<E> {
    private ArrayList<Iterator<E>> sequence = new ArrayList<Iterator<E>>();
    private int index = 0;
    public void add(Iterator<E> next) { sequence.add(next); }

    public boolean hasNext() {
	if(index < sequence.size()) {
	    Iterator<E> curIt = sequence.get(index);
	    while(!curIt.hasNext() && (index+1) < sequence.size())
		curIt = sequence.get(++index);
	    return curIt.hasNext();
	}
	else
	    return false;
    }
    public E next() {
	if(index < sequence.size()) {
	    Iterator<E> curIt = sequence.get(index);
	    while(!curIt.hasNext() && (index+1) < sequence.size())
		curIt = sequence.get(++index);
	    return curIt.next();
	}
	else
	    throw new NoSuchElementException();
	
    }
    public void remove() {
	throw new UnsupportedOperationException();
    }
}