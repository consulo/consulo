// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.util.query;

import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.concurrent.AsyncFuture;
import consulo.util.lang.function.Functions;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

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
        this(original, ContainerUtil.canonicalStrategy(), Functions.identity());
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
    protected boolean processResults(@Nonnull Processor<? super T> consumer) {
        return AbstractQuery.delegateProcessResults(
            myOriginal,
            new MyProcessor(Collections.synchronizedSet(Sets.newHashSet(myHashingStrategy)), consumer)
        );
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<? super T> consumer) {
        return myOriginal.forEachAsync(new MyProcessor(Collections.synchronizedSet(Sets.newHashSet(myHashingStrategy)), consumer));
    }

    private class MyProcessor implements Processor<T> {
        private final Set<? super M> myProcessedElements;
        private final Processor<? super T> myConsumer;

        MyProcessor(@Nonnull Set<? super M> processedElements, @Nonnull Processor<? super T> consumer) {
            myProcessedElements = processedElements;
            myConsumer = consumer;
        }

        @Override
        public boolean process(final T t) {
            ProgressManager.checkCanceled();
            // in case of exception do not mark the element as processed, we couldn't recover otherwise
            M m = myMapper.apply(t);
            if (myProcessedElements.contains(m)) {
                return true;
            }
            boolean result = myConsumer.process(t);
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
