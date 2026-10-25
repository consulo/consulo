// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Various ui states which shared between the main ui and the combined diff viewer
 */
public class CombinedDiffUIState {
    private boolean mySearchMode;
    private boolean myStickyHeaderUnderBorder;

    private DiffInfoState myDiffInfoState = EmptyDiffInfoState.INSTANCE;

    private final List<Consumer<DiffInfoState>> myDiffInfoListeners = new CopyOnWriteArrayList<>();

    public DiffInfoState getDiffInfoState() {
        return myDiffInfoState;
    }

    public void addDiffInfoListener(Consumer<DiffInfoState> listener, Disposable disposable) {
        myDiffInfoListeners.add(listener);
        Disposer.register(disposable, () -> myDiffInfoListeners.remove(listener));
    }

    public boolean isSeparatorVisible() {
        return mySearchMode || myStickyHeaderUnderBorder;
    }

    public void setSearchMode(boolean isSearchMode) {
        mySearchMode = isSearchMode;
    }

    public void setStickyHeaderUnderBorder(boolean isHeaderUnderBorder) {
        myStickyHeaderUnderBorder = isHeaderUnderBorder;
    }

    @RequiredUIAccess
    public void reset() {
        setStickyHeaderUnderBorder(false);
        setSearchMode(false);
        setDiffInfo(EmptyDiffInfoState.INSTANCE);
    }

    @RequiredUIAccess
    public void setDiffInfo(DiffInfoState diffInfoState) {
        if (myDiffInfoState.equals(diffInfoState)) {
            return;
        }
        myDiffInfoState = diffInfoState;
        for (Consumer<DiffInfoState> listener : myDiffInfoListeners) {
            listener.accept(diffInfoState);
        }
    }
}
