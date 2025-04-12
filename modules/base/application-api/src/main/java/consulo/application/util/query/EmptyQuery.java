// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.query;

import consulo.util.concurrent.AsyncFuture;
import consulo.util.concurrent.AsyncUtil;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author max
 */
public class EmptyQuery<R> implements Query<R> {
    private static final EmptyQuery EMPTY_QUERY_INSTANCE = new EmptyQuery();

    @Override
    @Nonnull
    public Collection<R> findAll() {
        return Collections.emptyList();
    }

    @Override
    public R findFirst() {
        return null;
    }

    @Override
    public boolean forEach(@Nonnull Predicate<? super R> consumer) {
        return true;
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull Predicate<? super R> consumer) {
        return AsyncUtil.wrapBoolean(true);
    }

    @Nonnull
    @Override
    public R[] toArray(@Nonnull R[] a) {
        return findAll().toArray(a);
    }

    @Nonnull
    @Override
    public Iterator<R> iterator() {
        return findAll().iterator();
    }

    public static <T> Query<T> getEmptyQuery() {
        @SuppressWarnings("unchecked") Query<T> instance = (Query<T>)EMPTY_QUERY_INSTANCE;
        return instance;
    }
}
