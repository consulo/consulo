// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.review.util;

import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SingleCoroutineLauncher {
    private final AtomicReference<@Nullable Object> myCurrentTaskKey = new AtomicReference<>();
    private final MutableObservableValue<Boolean> myBusy = MutableObservableValue.of(false);
    private volatile @Nullable CompletableFuture<?> myCurrentJob;

    public ObservableValue<Boolean> getBusy() {
        return myBusy;
    }

    public void launch(Supplier<? extends CompletableFuture<?>> block) {
        Object key = new Object();
        if (!myCurrentTaskKey.compareAndSet(null, key)) {
            return;
        }
        myBusy.set(true);
        CompletableFuture<?> job;
        try {
            job = block.get();
        }
        catch (Throwable e) {
            finish(key);
            throw e;
        }
        myCurrentJob = job;
        job.whenComplete((result, error) -> finish(key));
    }

    public void cancel() {
        CompletableFuture<?> job = myCurrentJob;
        if (job != null) {
            job.cancel(false);
        }
        myCurrentJob = null;
    }

    private void finish(Object key) {
        myCurrentJob = null;
        if (myCurrentTaskKey.compareAndSet(key, null)) {
            myBusy.set(false);
        }
    }
}
