// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.proxy.EventDispatcher;
import jakarta.annotation.Nonnull;

import java.util.EventListener;

@FunctionalInterface
public interface SimpleEventListener extends EventListener {
    void eventOccurred();

    static void addDisposableListener(
        @Nonnull EventDispatcher<SimpleEventListener> dispatcher,
        @Nonnull Disposable disposable,
        @Nonnull Runnable listener
    ) {
        dispatcher.addListener(listener::run, disposable);
    }

    static void addListener(
        @Nonnull EventDispatcher<SimpleEventListener> dispatcher,
        @Nonnull Runnable listener
    ) {
        dispatcher.addListener(listener::run);
    }

    static void addAndInvokeListener(
        @Nonnull EventDispatcher<SimpleEventListener> dispatcher,
        @Nonnull Runnable listener
    ) {
        dispatcher.addListener(listener::run);
        listener.run();
    }

    static void addAndInvokeListener(
        @Nonnull EventDispatcher<SimpleEventListener> dispatcher,
        @Nonnull Disposable disposable,
        @Nonnull Runnable listener
    ) {
        dispatcher.addListener(listener::run, disposable);
        if (!Disposer.isDisposed(disposable)) {
            listener.run();
        }
    }
}
