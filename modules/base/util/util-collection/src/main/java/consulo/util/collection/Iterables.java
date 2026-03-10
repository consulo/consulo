/*
 * Copyright 2013-2025 consulo.io
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

import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-06-21
 */
public class Iterables {
    @Contract(pure = true)
    public static <T> Iterable<T> iterate(T[] arrays, Predicate<? super T> condition) {
        return iterate(Arrays.asList(arrays), condition);
    }

    @Contract(pure = true)
    public static <T> Iterable<T> iterate(
        Collection<? extends T> collection,
        Predicate<? super T> condition
    ) {
        if (collection.isEmpty()) {
            return List.of();
        }
        return new Iterable<>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    private final Iterator<? extends T> impl = collection.iterator();
                    private T next = findNext();

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public T next() {
                        T result = next;
                        next = findNext();
                        return result;
                    }

                    @Nullable
                    private T findNext() {
                        while (impl.hasNext()) {
                            T each = impl.next();
                            if (condition.test(each)) {
                                return each;
                            }
                        }
                        return null;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
