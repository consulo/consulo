// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.util.query;

import consulo.application.progress.ProgressManager;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.concurrent.AsyncFuture;
import consulo.util.lang.function.Functions;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author max
 */
public class UniqueResultsQuery<T, M> extends AbstractQuery<T> {
    @Nonnull
    private final Query<? extends T> myOriginal;
    @Nonnull
    private final HashingStrategy<? super M> myHashingStrategy;
    @Nonnull
    private final Function<? super T, ? extends M> myMapper;

    public UniqueResultsQuery(@Nonnull Query<? extends T> original) {
        this(original, HashingStrategy.canonical(), Functions.identity());
    }

    public UniqueResultsQuery(@Nonnull Query<? extends T> original, @Nonnull HashingStrategy<? super M> hashingStrategy) {
        this(original, hashingStrategy, Functions.identity());
    }

    public UniqueResultsQuery(
        @Nonnull Query<? extends T> original,
        @Nonnull HashingStrategy<? super M> hashingStrategy,
        @Nonnull Function<? super T, ? extends M> mapper
    ) {
        myOriginal = original;
        myHashingStrategy = hashingStrategy;
        myMapper = mapper;
    }

    @Override
    protected boolean processResults(@Nonnull Predicate<? super T> consumer) {
        return AbstractQuery.delegateProcessResults(
            myOriginal,
            new MyProcessor(Collections.synchronizedSet(Sets.newHashSet(myHashingStrategy)), consumer)
        );
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull Predicate<? super T> consumer) {
        return myOriginal.forEachAsync(new MyProcessor(Collections.synchronizedSet(Sets.newHashSet(myHashingStrategy)), consumer));
    }

    private class MyProcessor implements Predicate<T> {
        private final Set<? super M> myProcessedElements;
        private final Predicate<? super T> myConsumer;

        MyProcessor(@Nonnull Set<? super M> processedElements, @Nonnull Predicate<? super T> consumer) {
            myProcessedElements = processedElements;
            myConsumer = consumer;
        }

        @Override
        public boolean test(T t) {
            ProgressManager.checkCanceled();
            // in case of exception do not mark the element as processed, we couldn't recover otherwise
            M m = myMapper.apply(t);
            if (myProcessedElements.contains(m)) {
                return true;
            }
            boolean result = myConsumer.test(t);
            myProcessedElements.add(m);
            return result;
        }
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public String toString() {
        return "UniqueQuery: " + myOriginal;
    }
}
