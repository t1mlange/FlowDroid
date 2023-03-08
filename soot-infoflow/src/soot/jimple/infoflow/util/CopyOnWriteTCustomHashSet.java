package soot.jimple.infoflow.util;

import gnu.trove.impl.hash.TObjectHash;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TCustomHashSet that shallow copies the inner array on add() and provides a thread-safe way to iterate.
 * WARNING: Only synchronizes add()!
 *
 * @param <E>
 */
public class CopyOnWriteTCustomHashSet<E> extends TCustomHashSet<E> {
    public CopyOnWriteTCustomHashSet(HashingStrategy<E> strategy) {
        super(strategy);
    }

    public CopyOnWriteTCustomHashSet() {
        super();
    }

    @Override
    public boolean add(E e) {
        synchronized (this) {
            Object[] newSet = new Object[_set.length];
            System.arraycopy(_set, 0, newSet, 0, _set.length);
            this._set = newSet;
            return super.add(e);
        }
    }

    /**
     * Iterator for the internal _set array of TCustomHashSet.
     *
     * @param <E>
     */
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
        Object[] set;
        synchronized (this) {
            set = this._set;
        }
        return new MyTCustomHashSetIterator<>(set);
    }
}
