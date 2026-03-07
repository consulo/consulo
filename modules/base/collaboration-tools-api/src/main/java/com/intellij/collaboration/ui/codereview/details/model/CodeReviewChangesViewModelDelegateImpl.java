// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.async.MapStateKt;
import com.intellij.collaboration.util.ComputedResult;
import com.intellij.collaboration.util.RefComparisonChange;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class CodeReviewChangesViewModelDelegateImpl<T> implements CodeReviewChangesViewModelDelegate<T> {
    private final CoroutineScope cs;
    private final Function3<? super CoroutineScope, ? super CodeReviewChangesContainer, ? super CodeReviewChangeList, ? extends T>
        vmProducer;
    private final MutableStateFlow<Result<State<T>>> state;
    private final StateFlow<String> selectedCommit;
    private final StateFlow<ComputedResult<T>> changeListVm;

    CodeReviewChangesViewModelDelegateImpl(
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<Result<CodeReviewChangesContainer>> changesContainer,
        @Nonnull Function3<? super CoroutineScope, ? super CodeReviewChangesContainer, ? super CodeReviewChangeList, ? extends T> vmProducer
    ) {
        this.cs = cs;
        this.vmProducer = vmProducer;
        this.state = StateFlowKt.MutableStateFlow(null);

        this.selectedCommit = MapStateKt.mapState(
            state,
            stateResult -> {
                if (stateResult == null) {
                    return null;
                }
                try {
                    return stateResult.getOrThrow().commit;
                }
                catch (Throwable e) {
                    return null;
                }
            }
        );

        this.changeListVm = MapStateKt.mapState(state, stateResult -> {
            if (stateResult == null) {
                return ComputedResult.loading();
            }
            try {
                return ComputedResult.success(stateResult.getOrThrow().vm);
            }
            catch (Throwable e) {
                return ComputedResult.failure(e);
            }
        });

        kotlinx.coroutines.BuildersKt.launch(
            cs,
            cs.getCoroutineContext(),
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (scope, continuation) -> FlowKt.collect(
                changesContainer,
                result -> {
                    State<T> oldState = null;
                    try {
                        Result<State<T>> old = state.getValue();
                        if (old != null) {
                            oldState = old.getOrThrow();
                        }
                    }
                    catch (Throwable ignored) {
                    }
                    if (oldState != null) {
                        CoroutineScopeKt.cancel(oldState.vmCs, null);
                    }
                    try {
                        CodeReviewChangesContainer changes = result.getOrThrow();
                        state.setValue(Result.success(createState(changes, null)));
                    }
                    catch (Throwable e) {
                        state.setValue(Result.failure(e));
                    }
                    return Unit.INSTANCE;
                },
                continuation
            )
        );
    }

    @Override
    public @Nonnull StateFlow<String> getSelectedCommit() {
        return selectedCommit;
    }

    @Override
    public @Nonnull StateFlow<ComputedResult<T>> getChangeListVm() {
        return changeListVm;
    }

    @Override
    public @Nullable T selectCommit(int index) {
        State<T> result = updateState(s -> {
            String commit = index >= 0 && index < s.changes.getCommits().size() ? s.changes.getCommits().get(index) : null;
            return changeCommit(s, commit);
        });
        return result != null ? result.vm : null;
    }

    @Override
    public @Nullable T selectCommit(@Nullable String commitSha) {
        State<T> result = updateState(s -> changeCommit(s, commitSha));
        return result != null ? result.vm : null;
    }

    @Override
    public @Nullable T selectNextCommit() {
        State<T> result = updateState(s -> {
            List<String> commits = s.changes.getCommits();
            if (s.commit == null) {
                return s;
            }
            int idx = commits.indexOf(s.commit);
            String nextCommit = (idx >= 0 && idx + 1 < commits.size()) ? commits.get(idx + 1) : null;
            if (nextCommit == null) {
                return s;
            }
            return changeCommit(s, nextCommit);
        });
        return result != null ? result.vm : null;
    }

    @Override
    public @Nullable T selectPreviousCommit() {
        State<T> result = updateState(s -> {
            List<String> commits = s.changes.getCommits();
            if (s.commit == null) {
                return s;
            }
            int idx = commits.indexOf(s.commit);
            String prevCommit = (idx > 0) ? commits.get(idx - 1) : null;
            if (prevCommit == null) {
                return s;
            }
            return changeCommit(s, prevCommit);
        });
        return result != null ? result.vm : null;
    }

    private CodeReviewChangeList getChangeList(@Nonnull CodeReviewChangesContainer changes, @Nullable String commit) {
        if (commit == null) {
            return new CodeReviewChangeList(null, changes.getSummaryChanges());
        }
        else {
            List<RefComparisonChange> commitChanges = changes.getChangesByCommits().get(commit);
            return new CodeReviewChangeList(commit, commitChanges != null ? commitChanges : Collections.emptyList());
        }
    }

    private @Nullable State<T> updateState(@Nonnull Function1<State<T>, State<T>> function) {
        Result<State<T>> updated = FlowKt.updateAndGet(state, current -> {
            if (current == null) {
                return null;
            }
            try {
                return Result.success(function.invoke(current.getOrThrow()));
            }
            catch (Throwable e) {
                return current;
            }
        });
        if (updated == null) {
            return null;
        }
        try {
            return updated.getOrThrow();
        }
        catch (Throwable e) {
            return null;
        }
    }

    private @Nonnull State<T> changeCommit(@Nonnull State<T> currentState, @Nullable String newCommit) {
        if (Objects.equals(newCommit, currentState.commit)) {
            return currentState;
        }
        CoroutineScopeKt.cancel(currentState.vmCs, null);
        return createState(currentState.changes, newCommit);
    }

    private @Nonnull State<T> createState(@Nonnull CodeReviewChangesContainer changes, @Nullable String commit) {
        CoroutineScope newCs = com.intellij.platform.util.coroutines.ChildScopeKt.childScope(cs, "Change List View Model");
        T vm = vmProducer.invoke(newCs, changes, getChangeList(changes, commit));
        return new State<>(changes, commit, newCs, vm);
    }

    private static final class State<T> {
        final CodeReviewChangesContainer changes;
        final @Nullable String commit;
        final CoroutineScope vmCs;
        final T vm;

        State(
            @Nonnull CodeReviewChangesContainer changes,
            @Nullable String commit,
            @Nonnull CoroutineScope vmCs,
            @Nonnull T vm
        ) {
            this.changes = changes;
            this.commit = commit;
            this.vmCs = vmCs;
            this.vm = vm;
        }
    }
}
