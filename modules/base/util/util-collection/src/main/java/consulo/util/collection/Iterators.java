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

import consulo.util.collection.impl.EmptyIterator;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * @author VISTALL
 * @since 13-Mar-22
 */
public final class Iterators {
    @Nonnull
    public static <T> Iterator<T> empty() {
        return EmptyIterator.getInstance();
    }

    @Nonnull
    @Contract(pure = true)
    public static <U> Iterator<U> mapIterator(@Nonnull PrimitiveIterator.OfInt iterator, @Nonnull IntFunction<? extends U> mapper) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Contract(pure = true)
    @Nonnull
    public static <T, U> Iterator<U> mapIterator(
        @Nonnull Iterator<? extends T> iterator,
        @Nonnull Function<? super T, ? extends U> mapper
    ) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }
}
