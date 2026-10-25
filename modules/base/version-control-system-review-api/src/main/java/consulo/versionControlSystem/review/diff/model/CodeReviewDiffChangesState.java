// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The changes a diff shows and the scroll requests for them.
 *
 * @param <C> change type
 */
public final class CodeReviewDiffChangesState<C> implements DiffViewerScrollRequestProducer {
    private final ListSelection<C> mySelectedChanges;

    private final List<Consumer<DiffViewerScrollRequest>> myListeners = new ArrayList<>();
    private @Nullable DiffViewerScrollRequest myPendingRequest;

    public CodeReviewDiffChangesState() {
        this(ListSelection.empty());
    }

    public CodeReviewDiffChangesState(ListSelection<C> selectedChanges) {
        mySelectedChanges = selectedChanges;
    }

    public ListSelection<C> getSelectedChanges() {
        return mySelectedChanges;
    }

    public void scroll(DiffViewerScrollRequest cmd) {
        Consumer<DiffViewerScrollRequest> listener;
        synchronized (myListeners) {
            if (myListeners.isEmpty()) {
                myPendingRequest = cmd;
                return;
            }
            listener = myListeners.get(myListeners.size() - 1);
        }
        listener.accept(cmd);
    }

    @Override
    public void handleScrollRequests(Disposable parentDisposable, Consumer<DiffViewerScrollRequest> listener) {
        DiffViewerScrollRequest pending;
        synchronized (myListeners) {
            myListeners.add(listener);
            pending = myPendingRequest;
            myPendingRequest = null;
        }
        Disposer.register(parentDisposable, () -> {
            synchronized (myListeners) {
                myListeners.remove(listener);
            }
        });
        if (pending != null) {
            listener.accept(pending);
        }
    }
}
