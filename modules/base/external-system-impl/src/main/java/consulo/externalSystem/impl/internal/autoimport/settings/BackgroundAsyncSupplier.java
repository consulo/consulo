// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.settings;

import consulo.application.internal.BackgroundTaskUtil;
import consulo.disposer.Disposable;

import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class BackgroundAsyncSupplier<R> implements AsyncSupplier<R> {
    private final AsyncSupplier<R> mySupplier;
    private final BooleanSupplier myShouldKeepTasksAsynchronous;
    private final Executor myBackgroundExecutor;
    private final Disposable myParentDisposable;

    public BackgroundAsyncSupplier(
        AsyncSupplier<R> supplier,
        BooleanSupplier shouldKeepTasksAsynchronous,
        Executor backgroundExecutor,
        Disposable parentDisposable
    ) {
        mySupplier = supplier;
        myShouldKeepTasksAsynchronous = shouldKeepTasksAsynchronous;
        myBackgroundExecutor = backgroundExecutor;
        myParentDisposable = parentDisposable;
    }

    @Override
    public void supply(Consumer<R> consumer) {
        if (myShouldKeepTasksAsynchronous.getAsBoolean()) {
            BackgroundTaskUtil.execute(myBackgroundExecutor, myParentDisposable, () -> mySupplier.supply(consumer));
        }
        else {
            mySupplier.supply(consumer);
        }
    }
}
