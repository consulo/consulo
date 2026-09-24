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
package consulo.ui.impl;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
final class ProtectedUIAccess extends BaseUIAccess {
    private final BaseUIAccess myOriginal;
    private final BooleanSupplier myDisposed;

    ProtectedUIAccess(BaseUIAccess original, BooleanSupplier disposed) {
        myOriginal = original;
        myDisposed = disposed;
    }

    private Runnable protect(Runnable runnable) {
        return () -> {
            if (!myDisposed.getAsBoolean()) {
                runnable.run();
            }
        };
    }

    @Override
    public UIAccess makeProtection(BooleanSupplier disposed) {
        return new ProtectedUIAccess(myOriginal, disposed);
    }

    @Override
    public UIAccess getOriginal() {
        return myOriginal;
    }

    @Override
    public boolean isValid() {
        return !myDisposed.getAsBoolean() && myOriginal.isValid();
    }

    @Override
    public void give(Runnable runnable) {
        myOriginal.give(protect(runnable));
    }

    @Override
    public void giveLater(Runnable runnable, ModalityState modalityState) {
        myOriginal.giveLater(protect(runnable), modalityState);
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        CompletableFuture<T> result = new CompletableFuture<>();
        myOriginal.giveAsync(() -> {
            if (myDisposed.getAsBoolean()) {
                result.cancel(false);
            }
            else {
                result.complete(supplier.get());
            }
            return null;
        }).whenComplete((ignored, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    @Override
    public void giveAndWait(Runnable runnable) {
        myOriginal.giveAndWait(protect(runnable));
    }

    @Override
    public void giveIfNeed(Runnable runnable) {
        myOriginal.giveIfNeed(protect(runnable));
    }

    @Override
    public void giveAndWaitIfNeed(Runnable runnable) {
        myOriginal.giveAndWaitIfNeed(protect(runnable));
    }

    @Override
    public void execute(Runnable command) {
        myOriginal.execute(protect(command));
    }

    @Override
    public boolean isInModalContext() {
        return myOriginal.isInModalContext();
    }

    @Override
    public int getEventCount() {
        return myOriginal.getEventCount();
    }

    @Override
    public Runnable markEventCount() {
        return myOriginal.markEventCount();
    }

    @Override
    public Clipboard getClipboard() {
        return myOriginal.getClipboard();
    }

    @Override
    protected Clipboard createClipboard() {
        return myOriginal.getClipboard();
    }

    @Override
    protected SingleUIAccessScheduler createScheduler() {
        ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
        return new SingleUIAccessScheduler(this, concurrency.getScheduledExecutorService());
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return myOriginal.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        myOriginal.putUserData(key, value);
    }

    @Override
    public <T> T putUserDataIfAbsent(Key<T> key, T value) {
        return myOriginal.putUserDataIfAbsent(key, value);
    }

    @Override
    public <T> boolean replace(Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
        return myOriginal.replace(key, oldValue, newValue);
    }
}
