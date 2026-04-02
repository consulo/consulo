// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.async.ChildScopeKt;
import com.intellij.collaboration.ui.SimpleEventListener;
import com.intellij.collaboration.util.ChangesSelection;
import com.intellij.collaboration.util.RefComparisonChange;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@ApiStatus.Internal
public abstract class CodeReviewChangeListViewModelBase implements CodeReviewChangeListViewModel {
    protected final CoroutineScope cs;
    protected final CodeReviewChangeList changeList;

    private final MutableSharedFlow<SelectionRequest> _selectionRequests;
    private final SharedFlow<SelectionRequest> selectionRequests;

    private final MutableStateFlow<ChangesSelection> _changesSelection;
    private final StateFlow<ChangesSelection> changesSelection;
    private final EventDispatcher<SimpleEventListener> selectionMulticaster =
        EventDispatcher.create(SimpleEventListener.class);

    protected final @Nullable String selectedCommit;

    private final List<RefComparisonChange> changes;

    private final ReentrantLock stateGuard = new ReentrantLock();

    protected CodeReviewChangeListViewModelBase(
        @Nonnull CoroutineScope parentCs,
        @Nonnull CodeReviewChangeList changeList
    ) {
        this.changeList = changeList;
        this.cs = ChildScopeKt.childScope(parentCs, getClass());

        this._selectionRequests = SharedFlowKt.MutableSharedFlow(1, 0, BufferOverflow.DROP_OLDEST);
        this.selectionRequests = FlowKt.asSharedFlow(_selectionRequests);

        this._changesSelection = StateFlowKt.MutableStateFlow(null);
        this.changesSelection = FlowKt.asStateFlow(_changesSelection);

        this.selectedCommit = changeList.getCommitSha();
        this.changes = changeList.getChanges();
    }

    @Override
    public final @Nonnull List<RefComparisonChange> getChanges() {
        return changes;
    }

    @Override
    public @Nonnull SharedFlow<SelectionRequest> getSelectionRequests() {
        return selectionRequests;
    }

    @Override
    public @Nonnull StateFlow<ChangesSelection> getChangesSelection() {
        return changesSelection;
    }

    public void selectChange(@Nullable RefComparisonChange change) {
        stateGuard.lock();
        try {
            if (change == null) {
                _changesSelection.setValue(new ChangesSelection.Fuzzy(changeList.getChanges()));
                _selectionRequests.tryEmit(SelectionRequest.All.INSTANCE);
            }
            else {
                if (!changeList.getChanges().contains(change)) {
                    return;
                }
                ChangesSelection currentSelection = _changesSelection.getValue();
                if (currentSelection == null || !(currentSelection instanceof ChangesSelection.Fuzzy fuzzy) || !fuzzy.getChanges()
                    .contains(change)) {
                    _changesSelection.setValue(new ChangesSelection.Precise(changeList.getChanges(), change));
                    _selectionRequests.tryEmit(new SelectionRequest.OneChange(change));
                }
            }
        }
        finally {
            stateGuard.unlock();
        }
    }

    @Override
    public void updateSelectedChanges(@Nullable ChangesSelection selection) {
        // do not update selection when change update is in progress
        if (!stateGuard.tryLock()) {
            return;
        }
        try {
            _changesSelection.setValue(selection);
            selectionMulticaster.getMulticaster().eventOccurred();
        }
        finally {
            stateGuard.unlock();
        }
    }

    /**
     * Listener invoked SYNCHRONOUSLY when selection is changed.
     * This is a suspend function that never returns (Nothing).
     */
    public Object handleSelection(
        @Nonnull kotlin.jvm.functions.Function1<? super ChangesSelection, Unit> listener,
        @Nonnull Continuation<? super Void> continuation
    ) {
        SimpleEventListener simpleListener = new SimpleEventListener() {
            @Override
            public void eventOccurred() {
                listener.invoke(changesSelection.getValue());
            }
        };
        try {
            selectionMulticaster.addListener(simpleListener);
            listener.invoke(changesSelection.getValue());
            return kotlinx.coroutines.AwaitCancellationKt.awaitCancellation(continuation);
        }
        finally {
            selectionMulticaster.removeListener(simpleListener);
        }
    }
}
