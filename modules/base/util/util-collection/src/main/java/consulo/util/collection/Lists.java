/*
 * Copyright 2013-2022 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.util.collection;

import consulo.util.collection.impl.list.LockFreeCopyOnWriteArrayList;
import consulo.util.collection.impl.list.SortedList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.ToIntFunction;

import static consulo.util.collection.ContainerUtil.addIfNotNull;
import static consulo.util.collection.ContainerUtil.swapElements;

/**
 * @author VISTALL
 * @since 2022-01-10
 */
public final class Lists {
    /**
     * Creates List which is thread-safe to modify and iterate.
     * It differs from the java.util.concurrent.CopyOnWriteArrayList in the following:
     * - faster modification in the uncontended case
     * - less memory
     * - slower modification in highly contented case (which is the kind of situation you shouldn't use COWAL anyway)
     * <p/>
     * N.B. Avoid using {@code list.toArray(new T[list.size()])} on this list because it is inherently racey and
     * therefore can return array with null elements at the end.
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> ConcurrentList<T> newLockFreeCopyOnWriteList() {
        return new LockFreeCopyOnWriteArrayList<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> ConcurrentList<T> newLockFreeCopyOnWriteList(@Nonnull Collection<? extends T> c) {
        return new LockFreeCopyOnWriteArrayList<>(c);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> newSortedList(@Nonnull Comparator<T> comparator) {
        return new SortedList<>(comparator);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> notNullize(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }

    public static <T> void weightSort(List<T> list, ToIntFunction<T> weighterFunc) {
        quickSort(list, (o1, o2) -> weighterFunc.applyAsInt(o2) - weighterFunc.applyAsInt(o1));
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> append(@Nonnull List<? extends T> list, @Nonnull T... values) {
        return ContainerUtil.concat(list, List.of(values));
    }

    // Generalized Quick Sort. Does neither array.clone() nor list.toArray()
    public static <T> void quickSort(@Nonnull List<T> list, @Nonnull Comparator<? super T> comparator) {
        quickSort(list, comparator, 0, list.size());
    }

    private static <T> void quickSort(@Nonnull List<T> x, @Nonnull Comparator<? super T> comparator, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && comparator.compare(x.get(j), x.get(j - 1)) < 0; j--) {
                    swapElements(x, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, comparator, l, l + s, l + 2 * s);
                m = med3(x, comparator, m - s, m, m + s);
                n = med3(x, comparator, n - 2 * s, n - s, n);
            }
            m = med3(x, comparator, l, m, n); // Mid-size, med of 3
        }
        T v = x.get(m);

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off;
        int b = a;
        int c = off + len - 1;
        int d = c;
        while (true) {
            while (b <= c && comparator.compare(x.get(b), v) <= 0) {
                if (comparator.compare(x.get(b), v) == 0) {
                    swapElements(x, a++, b);
                }
                b++;
            }
            while (c >= b && comparator.compare(v, x.get(c)) <= 0) {
                if (comparator.compare(x.get(c), v) == 0) {
                    swapElements(x, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swapElements(x, b++, c--);
        }

        // Swap partition elements back to middle
        int n = off + len;
        int s = Math.min(a - off, b - a);
        vecswap(x, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            quickSort(x, comparator, off, s);
        }
        if ((s = d - c) > 1) {
            quickSort(x, comparator, n - s, s);
        }
    }

    /*
     * Returns the index of the median of the three indexed longs.
     */
    private static <T> int med3(@Nonnull List<T> x, Comparator<? super T> comparator, int a, int b, int c) {
        return comparator.compare(x.get(a), x.get(b)) < 0
            ? comparator.compare(x.get(b), x.get(c)) < 0 ? b : comparator.compare(x.get(a), x.get(c)) < 0 ? c : a
            : comparator.compare(x.get(c), x.get(b)) < 0 ? b : comparator.compare(x.get(c), x.get(a)) < 0 ? c : a;
    }

    /*
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static <T> void vecswap(List<T> x, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swapElements(x, a, b);
        }
    }

    /**
     * @return read-only list consisting of the elements with nulls filtered out
     */
    @SafeVarargs
    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> packNullables(@Nonnull T... elements) {
        List<T> list = new ArrayList<>();
        for (T element : elements) {
            addIfNotNull(list, element);
        }
        return list.isEmpty() ? List.of() : list;
    }

    @Contract(pure = true)
    public static <T> int indexOfIdentity(@Nonnull List<? extends T> list, T element) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            if (list.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterable<T> iterateBackward(@Nonnull List<? extends T> list) {
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    private final ListIterator<? extends T> it = list.listIterator(list.size());

                    @Override
                    public boolean hasNext() {
                        return it.hasPrevious();
                    }

                    @Override
                    public T next() {
                        return it.previous();
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }
}
