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
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.util.ChangesSelection;
import consulo.versionControlSystem.review.util.FuzzyChangesSelection;
import consulo.versionControlSystem.review.util.PreciseChangesSelection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public abstract class CodeReviewChangeListViewModelBase implements CodeReviewChangeListViewModel {
    protected final Disposable cs;
    protected final CodeReviewChangeList changeList;

    private final List<Consumer<CodeReviewChangeListSelectionRequest>> mySelectionRequestListeners = new CopyOnWriteArrayList<>();
    private volatile @Nullable CodeReviewChangeListSelectionRequest myLastSelectionRequest;

    private final MutableObservableValue<@Nullable ChangesSelection> myChangesSelection = MutableObservableValue.of(null);
    private final List<Runnable> mySelectionMulticaster = new CopyOnWriteArrayList<>();

    protected final @Nullable String selectedCommit;

    private final ReentrantLock myStateGuard = new ReentrantLock();

    protected CodeReviewChangeListViewModelBase(Disposable parentCs, CodeReviewChangeList changeList) {
        this.changeList = changeList;
        cs = Disposable.newDisposable(getClass().getName());
        Disposer.register(parentCs, cs);
        selectedCommit = changeList.commitSha();
    }

    @Override
    public final List<RefComparisonChange> getChanges() {
        return changeList.changes();
    }

    @Override
    public void addSelectionRequestListener(Disposable parentDisposable, Consumer<CodeReviewChangeListSelectionRequest> listener) {
        mySelectionRequestListeners.add(listener);
        Disposer.register(parentDisposable, () -> mySelectionRequestListeners.remove(listener));
        CodeReviewChangeListSelectionRequest last = myLastSelectionRequest;
        if (last != null) {
            listener.accept(last);
        }
    }

    @Override
    public ObservableValue<@Nullable ChangesSelection> getChangesSelection() {
        return myChangesSelection;
    }

    public void selectChange(@Nullable RefComparisonChange change) {
        myStateGuard.lock();
        try {
            if (change == null) {
                myChangesSelection.set(new FuzzyChangesSelection(changeList.changes()));
                emitSelectionRequest(AllChangesSelectionRequest.INSTANCE);
            }
            else {
                if (!changeList.changes().contains(change)) {
                    return;
                }
                ChangesSelection currentSelection = myChangesSelection.get();
                if (!(currentSelection instanceof FuzzyChangesSelection fuzzy) || !fuzzy.getChanges().contains(change)) {
                    myChangesSelection.set(new PreciseChangesSelection(changeList.changes(), change));
                    emitSelectionRequest(new OneChangeSelectionRequest(change));
                }
            }
        }
        finally {
            myStateGuard.unlock();
        }
    }

    private void emitSelectionRequest(CodeReviewChangeListSelectionRequest request) {
        myLastSelectionRequest = request;
        for (Consumer<CodeReviewChangeListSelectionRequest> listener : mySelectionRequestListeners) {
            listener.accept(request);
        }
    }

    @Override
    public void updateSelectedChanges(@Nullable ChangesSelection selection) {
        // do not update selection when change update is in progress
        if (!myStateGuard.tryLock()) {
            return;
        }
        try {
            myChangesSelection.set(selection);
            for (Runnable listener : mySelectionMulticaster) {
                listener.run();
            }
        }
        finally {
            myStateGuard.unlock();
        }
    }

    /**
     * Listener invoked SYNCHRONOUSLY when selection is changed
     */
    public void handleSelection(Disposable parentDisposable, Consumer<@Nullable ChangesSelection> listener) {
        Runnable simpleListener = () -> listener.accept(myChangesSelection.get());
        mySelectionMulticaster.add(simpleListener);
        Disposer.register(parentDisposable, () -> mySelectionMulticaster.remove(simpleListener));
        listener.accept(myChangesSelection.get());
    }
}
