// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.util.query;

import consulo.util.concurrent.AsyncFuture;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * @author max
 */
public class FilteredQuery<T> extends AbstractQuery<T> {
    private final Query<T> myOriginal;
    private final Predicate<? super T> myFilter;

    public FilteredQuery(@Nonnull Query<T> original, @Nonnull Predicate<? super T> filter) {
        myOriginal = original;
        myFilter = filter;
    }

    @Override
    protected boolean processResults(@Nonnull Predicate<? super T> consumer) {
        return delegateProcessResults(myOriginal, new MyProcessor(consumer));
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull Predicate<? super T> consumer) {
        return myOriginal.forEachAsync(new MyProcessor(consumer));
    }

    private class MyProcessor implements Predicate<T> {
        private final Predicate<? super T> myConsumer;

        MyProcessor(@Nonnull Predicate<? super T> consumer) {
            myConsumer = consumer;
        }

        @Override
        public boolean test(T t) {
            return !myFilter.test(t) || myConsumer.test(t);
        }
    }
}
