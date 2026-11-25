package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;

/** Two operation-local snapshots; iteration only queries the fallback when it is reached. */
public final class LazySourceList<T> extends AbstractList<T> {
    private final Supplier<List<T>> firstSupplier, secondSupplier;
    private List<T> first, second;
    public LazySourceList(Supplier<List<T>> first, Supplier<List<T>> second) {
        this.firstSupplier = first; this.secondSupplier = second;
    }
    private List<T> first() { if (first == null) first = List.copyOf(firstSupplier.get()); return first; }
    private List<T> second() { if (second == null) second = List.copyOf(secondSupplier.get()); return second; }
    @Override public int size() { return first().size() + second().size(); }
    @Override public T get(int index) { return index < first().size() ? first().get(index) : second().get(index - first().size()); }
    @Override public Iterator<T> iterator() {
        return new Iterator<>() {
            int index;
            @Override public boolean hasNext() { return index < first().size() || index - first().size() < second().size(); }
            @Override public T next() { if (!hasNext()) throw new NoSuchElementException(); return get(index++); }
        };
    }
    @Override public Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED | Spliterator.NONNULL);
    }
}
