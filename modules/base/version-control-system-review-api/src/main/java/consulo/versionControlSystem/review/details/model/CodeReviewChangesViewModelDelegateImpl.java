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
package consulo.versionControlSystem.review.details.model;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.RefComparisonChange;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class CodeReviewChangesViewModelDelegateImpl<T> implements CodeReviewChangesViewModelDelegate<T> {
    private final Disposable myCs;
    private final CodeReviewChangeListVmProducer<T> myVmProducer;

    private final MutableObservableValue<@Nullable ComputedResult<State<T>>> myState = MutableObservableValue.of(null);

    private final ObservableValue<@Nullable String> mySelectedCommit;
    private final ObservableValue<ComputedResult<T>> myChangeListVm;

    CodeReviewChangesViewModelDelegateImpl(Disposable cs,
                                           ObservableValue<@Nullable ComputedResult<CodeReviewChangesContainer>> changesContainer,
                                           CodeReviewChangeListVmProducer<T> vmProducer) {
        myCs = cs;
        myVmProducer = vmProducer;
        mySelectedCommit = myState.<@Nullable String>map(it -> {
            State<T> state = it == null ? null : it.getOrNull();
            return state == null ? null : state.commit();
        });
        myChangeListVm = myState.map(it -> (it == null ? ComputedResult.<State<T>>loading() : it).map(State::vm));

        applyChanges(changesContainer.get());
        Runnable unsubscribe = changesContainer.addListener(this::applyChanges);
        Disposer.register(cs, unsubscribe::run);
    }

    private void applyChanges(@Nullable ComputedResult<CodeReviewChangesContainer> result) {
        State<T> current = currentState();
        if (current != null) {
            Disposer.dispose(current.vmCs());
        }
        myState.set(result == null || result.isInProgress() ? null : result.map(this::createState));
    }

    @Override
    public ObservableValue<@Nullable String> getSelectedCommit() {
        return mySelectedCommit;
    }

    @Override
    public ObservableValue<ComputedResult<T>> getChangeListVm() {
        return myChangeListVm;
    }

    @Override
    public @Nullable T selectCommit(int index) {
        State<T> state = updateState(it -> {
            List<String> commits = it.changes().getCommits();
            String commit = index >= 0 && index < commits.size() ? commits.get(index) : null;
            return changeCommit(it, commit);
        });
        return state == null ? null : state.vm();
    }

    @Override
    public @Nullable T selectCommit(@Nullable String commitSha) {
        State<T> state = updateState(it -> changeCommit(it, commitSha));
        return state == null ? null : state.vm();
    }

    @Override
    public @Nullable T selectNextCommit() {
        State<T> state = updateState(it -> {
            List<String> commits = it.changes().getCommits();
            String commit = it.commit();
            int idx = commit == null ? -1 : commits.indexOf(commit);
            if (idx < 0 || idx + 1 >= commits.size()) {
                return it;
            }
            return changeCommit(it, commits.get(idx + 1));
        });
        return state == null ? null : state.vm();
    }

    @Override
    public @Nullable T selectPreviousCommit() {
        State<T> state = updateState(it -> {
            List<String> commits = it.changes().getCommits();
            String commit = it.commit();
            int idx = commit == null ? -1 : commits.indexOf(commit);
            if (idx < 1) {
                return it;
            }
            return changeCommit(it, commits.get(idx - 1));
        });
        return state == null ? null : state.vm();
    }

    private static CodeReviewChangeList getChangeList(CodeReviewChangesContainer container, @Nullable String commit) {
        if (commit == null) {
            return new CodeReviewChangeList(null, container.getSummaryChanges());
        }
        Map<String, List<RefComparisonChange>> byCommits = container.getChangesByCommits();
        List<RefComparisonChange> changes = byCommits.get(commit);
        return new CodeReviewChangeList(commit, changes != null ? changes : List.of());
    }

    private @Nullable State<T> currentState() {
        ComputedResult<State<T>> result = myState.get();
        return result == null ? null : result.getOrNull();
    }

    private @Nullable State<T> updateState(Function<State<T>, State<T>> function) {
        ComputedResult<State<T>> updated = myState.updateAndGet(it -> it == null ? null : it.map(function));
        return updated == null ? null : updated.getOrNull();
    }

    private State<T> changeCommit(State<T> state, @Nullable String newCommit) {
        if (Objects.equals(newCommit, state.commit())) {
            return state;
        }
        Disposer.dispose(state.vmCs());
        return createState(state.changes(), newCommit);
    }

    private State<T> createState(CodeReviewChangesContainer changes) {
        return createState(changes, null);
    }

    private State<T> createState(CodeReviewChangesContainer changes, @Nullable String commit) {
        Disposable newCs = Disposable.newDisposable("Change List View Model");
        Disposer.register(myCs, newCs);
        return new State<>(changes, commit, newCs, myVmProducer.produce(newCs, changes, getChangeList(changes, commit)));
    }

    private record State<T>(CodeReviewChangesContainer changes, @Nullable String commit, Disposable vmCs, T vm) {
    }
}
