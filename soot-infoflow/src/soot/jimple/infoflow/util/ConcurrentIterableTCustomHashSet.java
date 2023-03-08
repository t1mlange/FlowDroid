package soot.jimple.infoflow.util;

import gnu.trove.impl.hash.TObjectHash;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Class that synchronizes access to add and size() and supports thread-safe iteration with threadSafeIterator().
 * WARNING: Only overrides add() and size()!
 *
 * @param <E>
 */
public class ConcurrentIterableTCustomHashSet<E> extends TCustomHashSet<E> {
    public ConcurrentIterableTCustomHashSet(HashingStrategy<E> strategy) {
        super(strategy);
    }

    public ConcurrentIterableTCustomHashSet() {
        super();
    }

    @Override
    public boolean add(E e) {
        synchronized (this) {
            return super.add(e);
        }
    }

    @Override
    public int size() {
        synchronized (this) {
            return super.size();
        }
    }

    static class MyTCustomHashSetIterator<E> implements Iterator<E> {
        Object[] arr;
        int index;

        MyTCustomHashSetIterator(Object[] arr) {
            this.arr = arr;
            this.index = arr.length;
        }

        @Override
        public boolean hasNext() {
            return this.nextIndex() >= 0;
        }

        @Override
        public E next() {
            this.moveToNextIndex();
            return (E) this.arr[this.index];
        }

        private void moveToNextIndex() {
            if ((this.index = this.nextIndex()) < 0) {
                throw new NoSuchElementException();
            }
        }

        private final int nextIndex() {
            int i = this.index;

            while(i-- > 0 && (arr[i] == TObjectHash.FREE || arr[i] == TObjectHash.REMOVED)) {
            }

            return i;
        }
    }

    public Iterator<E> threadSafeIterator() {
        synchronized (this) {
            return new MyTCustomHashSetIterator<>(this._set.clone());
        }
    }
}
