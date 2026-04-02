// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A helper class to reuse typesafe state mutation functionality
 */
public final class ReviewToolwindowTabsStateHolder<T extends ReviewTab, VM extends ReviewTabViewModel> {
    private final @Nonnull MutableStateFlow<ReviewToolwindowTabs<T, VM>> tabs;

    public ReviewToolwindowTabsStateHolder() {
        this(StateFlowKt.MutableStateFlow(new ReviewToolwindowTabs<>(Map.of(), null)));
    }

    public ReviewToolwindowTabsStateHolder(@Nonnull MutableStateFlow<ReviewToolwindowTabs<T, VM>> tabs) {
        this.tabs = tabs;
    }

    public @Nonnull MutableStateFlow<ReviewToolwindowTabs<T, VM>> getTabs() {
        return tabs;
    }

    public <_T extends T, _VM extends VM> void showTab(
        @Nonnull _T tab,
        @Nonnull Function<_T, _VM> vmProducer,
        @Nonnull Consumer<_VM> processVM
    ) {
        ReviewToolwindowTabs<T, VM> current = tabs.getValue();
        VM currentVm = current.getTabs().get(tab);
        if (currentVm == null || !tab.getReuseTabOnRequest()) {
            if (currentVm instanceof Disposable d) {
                Disposer.dispose(d);
            }
            _VM tabVm = vmProducer.apply(tab);
            processVM.accept(tabVm);
            Map<T, VM> newTabs = new HashMap<>(current.getTabs());
            newTabs.put(tab, tabVm);
            tabs.setValue(current.copy(newTabs, tab));
        }
        else {
            @SuppressWarnings("unchecked")
            _VM typedVm = (_VM) currentVm;
            processVM.accept(typedVm);
            tabs.setValue(current.copy(current.getTabs(), tab));
        }
    }

    public <_T extends T, _VM extends VM> void showTab(
        @Nonnull _T tab,
        @Nonnull Function<_T, _VM> vmProducer
    ) {
        showTab(tab, vmProducer, vm -> {
        });
    }

    public void select(@Nullable T tab) {
        ReviewToolwindowTabs<T, VM> current = tabs.getValue();
        tabs.setValue(current.copy(current.getTabs(), tab));
    }

    public void close(@Nonnull T tab) {
        ReviewToolwindowTabs<T, VM> current = tabs.getValue();
        VM currentVm = current.getTabs().get(tab);
        if (currentVm != null) {
            if (currentVm instanceof Disposable d) {
                Disposer.dispose(d);
            }
            Map<T, VM> newTabs = new HashMap<>(current.getTabs());
            newTabs.remove(tab);
            T selectedTab = tab.equals(current.getSelectedTab()) ? null : current.getSelectedTab();
            tabs.setValue(current.copy(newTabs, selectedTab));
        }
    }
}
