// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.util.query;

import consulo.application.util.function.Processor;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncFuture;
import consulo.util.concurrent.AsyncUtil;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author max
 */
public class ArrayQuery<T> implements Query<T> {
    private final T[] myArray;

    public ArrayQuery(@Nonnull T... array) {
        myArray = array;
    }

    @Override
    @Nonnull
    public Collection<T> findAll() {
        return Arrays.asList(myArray);
    }

    @Override
    public T findFirst() {
        return myArray.length > 0 ? myArray[0] : null;
    }

    @Override
    public boolean forEach(@Nonnull final Processor<? super T> consumer) {
        return ContainerUtil.process(myArray, consumer);
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull final Processor<? super T> consumer) {
        return AsyncUtil.wrapBoolean(forEach(consumer));
    }


    @Nonnull
    @Override
    public T[] toArray(@Nonnull final T[] a) {
        return myArray;
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(myArray).iterator();
    }
}
