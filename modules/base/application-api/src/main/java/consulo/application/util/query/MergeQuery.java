// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.application.util.query;

import consulo.util.concurrent.*;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

public class MergeQuery<T> extends AbstractQuery<T> {
    private final Query<? extends T> myQuery1;
    private final Query<? extends T> myQuery2;

    public MergeQuery(@Nonnull Query<? extends T> query1, @Nonnull Query<? extends T> query2) {
        myQuery1 = query1;
        myQuery2 = query2;
    }

    @Override
    protected boolean processResults(@Nonnull Predicate<? super T> consumer) {
        return delegateProcessResults(myQuery1, consumer) && delegateProcessResults(myQuery2, consumer);
    }

    @Nonnull
    @Override
    public AsyncFuture<Boolean> forEachAsync(@Nonnull Predicate<? super T> consumer) {
        AsyncFutureResult<Boolean> result = AsyncFutureFactory.getInstance().createAsyncFutureResult();

        AsyncFuture<Boolean> fq = myQuery1.forEachAsync(consumer);

        fq.addConsumer(SameThreadExecutor.INSTANCE, new DefaultResultConsumer<>(result) {
            @Override
            public void onSuccess(Boolean value) {
                if (value) {
                    AsyncFuture<Boolean> fq2 = myQuery2.forEachAsync(consumer);
                    fq2.addConsumer(SameThreadExecutor.INSTANCE, new DefaultResultConsumer<>(result));
                }
                else {
                    result.set(false);
                }
            }
        });
        return result;
    }
}
