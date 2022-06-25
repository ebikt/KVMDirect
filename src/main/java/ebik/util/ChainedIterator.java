package ebik.util;

import java.util.*;

public class ChainedIterator<E> implements Iterator<E>, Iterable<E> {
  private Vector<Iterator<E>> chain = new Vector<Iterator<E>>();

  public ChainedIterator() {}
  
  public ChainedIterator<E>add(Iterator<E> link) {
    chain.add(link);
    return this;
  }
  public ChainedIterator<E>add(Iterable<E> link) {
    chain.add(link.iterator());
    return this;
  }


  public boolean hasNext() {
    while (chain.size()>0) {
      if (chain.elementAt(0).hasNext()) return true;
      chain.removeElementAt(0);
    }
    return false;
  }

  public E next() throws NoSuchElementException {
    while (chain.size()>0) {
      try {
	return chain.elementAt(0).next();
      } catch (NoSuchElementException e) {
	chain.removeElementAt(0);
      }
    }
    throw new NoSuchElementException();
  }
  
  public Iterator<E> iterator() {
    return this;
  }
}