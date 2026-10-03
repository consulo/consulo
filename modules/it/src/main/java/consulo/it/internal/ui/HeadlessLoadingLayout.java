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
package consulo.it.internal.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.LoadingLayoutLoader;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.LoadingLayout;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 */
public class HeadlessLoadingLayout<L extends Layout> extends HeadlessLayoutBase<LayoutConstraint> implements LoadingLayout<L> {
    private final L myInnerLayout;
    private final Set<Future<?>> myPendingLoads = ConcurrentHashMap.newKeySet();

    private volatile boolean myDisposed;
    private boolean myLoading;
    private LocalizeValue myLoadingText = LocalizeValue.empty();

    public HeadlessLoadingLayout(L innerLayout, Disposable parent) {
        myInnerLayout = innerLayout;
        addChild(innerLayout);

        Disposer.register(parent, this::dispose);
    }

    public L getInnerLayout() {
        return myInnerLayout;
    }

    public boolean isLoading() {
        return myLoading;
    }

    public LocalizeValue getLoadingText() {
        return myLoadingText;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    @RequiredUIAccess
    public <Value> Future<Value> startLoading(Supplier<Value> valueGetter, BiConsumer<L, Value> uiSetter) {
        myPendingLoads.removeIf(Future::isDone);

        Future<Value> future = LoadingLayoutLoader.startLoading(this, valueGetter, uiSetter);
        if (myDisposed) {
            future.cancel(false);
        }
        else {
            myPendingLoads.add(future);
        }
        return future;
    }

    @Override
    @RequiredUIAccess
    public void startLoading() {
        myInnerLayout.removeAll();
        myLoading = true;
    }

    @Override
    @RequiredUIAccess
    public void startLoading(LocalizeValue loadingText) {
        myInnerLayout.removeAll();
        myLoadingText = loadingText;
        myLoading = true;
    }

    @Override
    @RequiredUIAccess
    public void stopLoading(Consumer<L> consumer) {
        myLoading = false;
        if (myDisposed) {
            return;
        }
        consumer.accept(myInnerLayout);
    }

    @Override
    @RequiredUIAccess
    public void setLoadingText(LocalizeValue loadingText) {
        myLoadingText = loadingText;
    }

    @Override
    @RequiredUIAccess
    public void remove(Component component) {
        myInnerLayout.remove(component);
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        myInnerLayout.removeAll();
    }

    private void dispose() {
        myDisposed = true;
        for (Future<?> future : myPendingLoads) {
            future.cancel(false);
        }
        myPendingLoads.clear();
    }
}
