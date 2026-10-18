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
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

final class CodeReviewCommitsChangesStateHandlerImpl<C, VM> implements CodeReviewCommitsChangesStateHandler<C, VM> {
    private final Disposable myCs;
    private final List<C> myCommits;
    private final BiFunction<Disposable, @Nullable C, VM> myCommitChangesVmProducer;

    private final MutableObservableValue<State<VM>> myState;

    private final ObservableValue<@Nullable C> mySelectedCommit;
    private final ObservableValue<VM> myChangeListVm;

    CodeReviewCommitsChangesStateHandlerImpl(Disposable cs,
                                             List<C> commits,
                                             BiFunction<Disposable, @Nullable C, VM> commitChangesVmProducer,
                                             int initialCommitIdx) {
        myCs = cs;
        myCommits = commits;
        myCommitChangesVmProducer = commitChangesVmProducer;
        myState = MutableObservableValue.of(createState(initialCommitIdx));
        mySelectedCommit = myState.<@Nullable C>map(it -> commitAt(it.commitIdx()));
        myChangeListVm = myState.map(State::vm);
    }

    @Override
    public ObservableValue<@Nullable C> getSelectedCommit() {
        return mySelectedCommit;
    }

    @Override
    public ObservableValue<VM> getChangeListVm() {
        return myChangeListVm;
    }

    @Override
    public @Nullable VM selectCommit(int index) {
        if (index > 0 && (index >= myCommits.size())) {
            return null;
        }
        return myState.updateAndGet(it -> changeCommit(it, index)).vm();
    }

    @Override
    public @Nullable VM selectCommit(@Nullable C commit) {
        int idx = myCommits.indexOf(commit);
        if (idx < 0) {
            return null;
        }
        return myState.updateAndGet(it -> changeCommit(it, idx)).vm();
    }

    @Override
    public @Nullable VM selectNextCommit() {
        int newIdx = myState.get().commitIdx() + 1;
        if (newIdx < 0 || newIdx >= myCommits.size()) {
            // return out of select, do not update
            return null;
        }
        return myState.updateAndGet(it -> changeCommit(it, newIdx)).vm();
    }

    @Override
    public @Nullable VM selectPreviousCommit() {
        int newIdx = myState.get().commitIdx() - 1;
        if (newIdx < 0 || newIdx >= myCommits.size()) {
            // return out of select, do not update
            return null;
        }
        return myState.updateAndGet(it -> changeCommit(it, newIdx)).vm();
    }

    private @Nullable C commitAt(int idx) {
        return idx >= 0 && idx < myCommits.size() ? myCommits.get(idx) : null;
    }

    private State<VM> changeCommit(State<VM> state, int commitIdx) {
        if (state.commitIdx() == commitIdx) {
            return state;
        }
        Disposer.dispose(state.vmCs());
        return createState(commitIdx);
    }

    private State<VM> createState(int commitIdx) {
        Disposable newCs = Disposable.newDisposable("Commit Changes View Model");
        Disposer.register(myCs, newCs);
        return new State<>(commitIdx, newCs, myCommitChangesVmProducer.apply(newCs, commitAt(commitIdx)));
    }

    private record State<VM>(int commitIdx, Disposable vmCs, VM vm) {
    }
}
