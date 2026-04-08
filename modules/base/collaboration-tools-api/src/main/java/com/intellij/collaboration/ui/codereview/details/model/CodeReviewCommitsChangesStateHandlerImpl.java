// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.async.MapStateKt;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

final class CodeReviewCommitsChangesStateHandlerImpl<C, VM> implements CodeReviewCommitsChangesStateHandler<C, VM> {
    private final CoroutineScope cs;
    private final List<C> commits;
    private final Function2<? super CoroutineScope, ? super C, ? extends VM> commitChangesVmProducer;
    private final MutableStateFlow<State<VM>> state;
    private final StateFlow<C> selectedCommit;
    private final StateFlow<VM> changeListVm;

    CodeReviewCommitsChangesStateHandlerImpl(
        @Nonnull CoroutineScope cs,
        @Nonnull List<C> commits,
        @Nonnull Function2<? super CoroutineScope, ? super C, ? extends VM> commitChangesVmProducer,
        int initialCommitIdx
    ) {
        this.cs = cs;
        this.commits = commits;
        this.commitChangesVmProducer = commitChangesVmProducer;
        this.state = StateFlowKt.MutableStateFlow(createState(initialCommitIdx));
        this.selectedCommit = MapStateKt.mapState(
            state,
            s -> {
                int idx = s.commitIdx;
                return (idx >= 0 && idx < commits.size()) ? commits.get(idx) : null;
            }
        );
        this.changeListVm = MapStateKt.mapState(state, s -> s.vm);
    }

    @Override
    public @Nonnull StateFlow<C> getSelectedCommit() {
        return selectedCommit;
    }

    @Override
    public @Nonnull StateFlow<VM> getChangeListVm() {
        return changeListVm;
    }

    @Override
    public @Nullable VM selectCommit(int index) {
        if (index > 0 && (index < 0 || index >= commits.size())) {
            return null;
        }
        State<VM> result = FlowKt.updateAndGet(state, s -> changeCommit(s, index));
        return result.vm;
    }

    @Override
    public @Nullable VM selectCommit(@Nullable C commit) {
        int idx = commits.indexOf(commit);
        if (idx < 0) {
            return null;
        }
        State<VM> result = FlowKt.updateAndGet(state, s -> changeCommit(s, idx));
        return result.vm;
    }

    @Override
    public @Nullable VM selectNextCommit() {
        State<VM> current = state.getValue();
        int newIdx = current.commitIdx + 1;
        if (newIdx < 0 || newIdx >= commits.size()) {
            return null;
        }
        State<VM> result = FlowKt.updateAndGet(state, s -> changeCommit(s, newIdx));
        return result.vm;
    }

    @Override
    public @Nullable VM selectPreviousCommit() {
        State<VM> current = state.getValue();
        int newIdx = current.commitIdx - 1;
        if (newIdx < 0 || newIdx >= commits.size()) {
            return null;
        }
        State<VM> result = FlowKt.updateAndGet(state, s -> changeCommit(s, newIdx));
        return result.vm;
    }

    private @Nonnull State<VM> changeCommit(@Nonnull State<VM> currentState, int commitIdx) {
        if (currentState.commitIdx == commitIdx) {
            return currentState;
        }
        CoroutineScopeKt.cancel(currentState.vmCs, null);
        return createState(commitIdx);
    }

    private @Nonnull State<VM> createState(int commitIdx) {
        CoroutineScope newCs = com.intellij.platform.util.coroutines.ChildScopeKt.childScope(cs, "Commit Changes View Model");
        C commit = (commitIdx >= 0 && commitIdx < commits.size()) ? commits.get(commitIdx) : null;
        VM vm = commitChangesVmProducer.invoke(newCs, commit);
        return new State<>(commitIdx, newCs, vm);
    }

    private static final class State<VM> {
        final int commitIdx;
        final CoroutineScope vmCs;
        final VM vm;

        State(int commitIdx, @Nonnull CoroutineScope vmCs, @Nonnull VM vm) {
            this.commitIdx = commitIdx;
            this.vmCs = vmCs;
            this.vm = vm;
        }
    }
}
