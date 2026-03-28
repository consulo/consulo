// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.util.query;

import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncFuture;
import consulo.util.concurrent.AsyncUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author max
 */
public class ArrayQuery<T> implements Query<T> {
    private final T[] myArray;

    public ArrayQuery(T... array) {
        myArray = array;
    }

    @Override
    public Collection<T> findAll() {
        return Arrays.asList(myArray);
    }

    @Override
    public @Nullable T findFirst() {
        return myArray.length > 0 ? myArray[0] : null;
    }

    @Override
    public boolean forEach(Predicate<? super T> consumer) {
        return ContainerUtil.process(myArray, consumer);
    }

    @Override
    public AsyncFuture<Boolean> forEachAsync(Predicate<? super T> consumer) {
        return AsyncUtil.wrapBoolean(forEach(consumer));
    }

    @Override
    public T[] toArray(T[] a) {
        return myArray;
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(myArray).iterator();
    }
}
