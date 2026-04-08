// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import consulo.application.Application;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.disposer.Disposable;
import consulo.proxy.EventDispatcher;
import jakarta.annotation.Nonnull;

public final class ListenableProgressIndicator extends ProgressIndicatorBase {
    private final EventDispatcher<SimpleEventListener> eventDispatcher = EventDispatcher.create(SimpleEventListener.class);

    @Override
    public boolean isReuseable() {
        return true;
    }

    @Override
    protected void onProgressChange() {
        Application.get().invokeAndWait(() -> eventDispatcher.getMulticaster().eventOccurred());
    }

    public void addAndInvokeListener(@Nonnull Runnable listener) {
        SimpleEventListener.addAndInvokeListener(eventDispatcher, listener);
    }

    public void addAndInvokeListener(@Nonnull Disposable disposable, @Nonnull Runnable listener) {
        SimpleEventListener.addAndInvokeListener(eventDispatcher, disposable, listener);
    }
}
