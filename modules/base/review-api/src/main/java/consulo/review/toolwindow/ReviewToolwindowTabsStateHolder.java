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
package consulo.review.toolwindow;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A helper class to reuse typesafe state mutation functionality
 */
public final class ReviewToolwindowTabsStateHolder<T extends ReviewTab, VM extends ReviewTabViewModel> {
    private final MutableObservableValue<ReviewToolwindowTabs<T, VM>> myTabs;

    public ReviewToolwindowTabsStateHolder() {
        this(MutableObservableValue.of(new ReviewToolwindowTabs<>(Map.of(), null)));
    }

    public ReviewToolwindowTabsStateHolder(MutableObservableValue<ReviewToolwindowTabs<T, VM>> tabs) {
        myTabs = tabs;
    }

    public MutableObservableValue<ReviewToolwindowTabs<T, VM>> getTabs() {
        return myTabs;
    }

    public <TT extends T, TVM extends VM> void showTab(TT tab, Class<TVM> vmClass, Function<TT, TVM> vmProducer) {
        showTab(tab, vmClass, vmProducer, vm -> {
        });
    }

    public <TT extends T, TVM extends VM> void showTab(TT tab, Class<TVM> vmClass, Function<TT, TVM> vmProducer, Consumer<TVM> processVM) {
        myTabs.update(current -> {
            VM currentVm = current.tabs().get(tab);
            if (currentVm == null || !vmClass.isInstance(currentVm) || !tab.isReuseTabOnRequest()) {
                dispose(currentVm);
                TVM tabVm = vmProducer.apply(tab);
                processVM.accept(tabVm);
                Map<T, VM> tabs = new LinkedHashMap<>(current.tabs());
                tabs.put(tab, tabVm);
                return new ReviewToolwindowTabs<>(Collections.unmodifiableMap(tabs), tab);
            }
            processVM.accept(vmClass.cast(currentVm));
            return new ReviewToolwindowTabs<>(current.tabs(), tab);
        });
    }

    /**
     * Close {@code tabToClose} and show {@code tab}
     */
    public <TT extends T, TVM extends VM> void showTabInstead(T tabToClose, TT tab, Class<TVM> vmClass, Function<TT, TVM> vmProducer) {
        showTabInstead(tabToClose, tab, vmClass, vmProducer, vm -> {
        });
    }

    public <TT extends T, TVM extends VM> void showTabInstead(T tabToClose,
                                                              TT tab,
                                                              Class<TVM> vmClass,
                                                              Function<TT, TVM> vmProducer,
                                                              Consumer<TVM> processVM) {
        myTabs.update(current -> {
            dispose(current.tabs().get(tabToClose));
            VM currentVm = current.tabs().get(tab);
            Map<T, VM> tabs = new LinkedHashMap<>(current.tabs());
            if (currentVm == null || !vmClass.isInstance(currentVm) || !tab.isReuseTabOnRequest()) {
                dispose(currentVm);
                TVM tabVm = vmProducer.apply(tab);
                processVM.accept(tabVm);
                tabs.put(tab, tabVm);
            }
            else {
                processVM.accept(vmClass.cast(currentVm));
            }
            tabs.remove(tabToClose);
            return new ReviewToolwindowTabs<>(Collections.unmodifiableMap(tabs), tab);
        });
    }

    public void select(@Nullable T tab) {
        myTabs.update(current -> new ReviewToolwindowTabs<>(current.tabs(), tab));
    }

    public void close(T tab) {
        myTabs.update(current -> {
            VM currentVm = current.tabs().get(tab);
            if (currentVm == null) {
                return current;
            }
            dispose(currentVm);
            Map<T, VM> tabs = new LinkedHashMap<>(current.tabs());
            tabs.remove(tab);
            T selected = current.selectedTab();
            return new ReviewToolwindowTabs<>(Collections.unmodifiableMap(tabs), tab.equals(selected) ? null : selected);
        });
    }

    private static void dispose(@Nullable Object vm) {
        if (vm instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }
    }
}
