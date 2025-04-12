// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.query;

import consulo.application.util.function.Processor;
import consulo.util.concurrent.AsyncFuture;

import jakarta.annotation.Nonnull;

/**
 * @param <S> source type
 * @param <T> target type
 */
public class InstanceofQuery<S, T> extends AbstractQuery<T> {
    private final Class<? extends T>[] myClasses;
    private final Query<S> myDelegate;

    public InstanceofQuery(Query<S> delegate, Class<? extends T>... aClasses) {
        myClasses = aClasses;
        myDelegate = delegate;
    }

    @Override
    protected boolean processResults(@Nonnull Processor<? super T> consumer) {
        return delegateProcessResults(myDelegate, new MyProcessor(consumer));
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull Processor<? super T> consumer) {
        return myDelegate.forEachAsync(new MyProcessor(consumer));
    }

    private class MyProcessor implements Processor<S> {
        private final Processor<? super T> myConsumer;

        MyProcessor(Processor<? super T> consumer) {
            myConsumer = consumer;
        }

        @Override
        public boolean process(S o) {
            for (Class<? extends T> aClass : myClasses) {
                if (aClass.isInstance(o)) {
                    //noinspection unchecked
                    return myConsumer.process((T)o);
                }
            }
            return true;
        }
    }
}
