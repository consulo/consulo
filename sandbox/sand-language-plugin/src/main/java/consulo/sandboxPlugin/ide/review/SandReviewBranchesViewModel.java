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
package consulo.sandboxPlugin.ide.review;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.details.model.CodeReviewBranches;
import consulo.versionControlSystem.review.details.model.CodeReviewBranchesViewModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewBranchesViewModel implements CodeReviewBranchesViewModel {
    private static final String SOURCE = "feature/review-demo";
    private static final String TARGET = "master";

    private final MutableObservableValue<String> mySourceBranch = MutableObservableValue.of(SOURCE);
    private final MutableObservableValue<Boolean> myCheckedOut = MutableObservableValue.of(false);
    private final List<Consumer<CodeReviewBranches>> myShowBranchesListeners = new CopyOnWriteArrayList<>();

    @Override
    public ObservableValue<String> getSourceBranch() {
        return mySourceBranch;
    }

    @Override
    public ObservableValue<Boolean> isCheckedOut() {
        return myCheckedOut;
    }

    @Override
    public void addShowBranchesRequestListener(Disposable parentDisposable, Consumer<CodeReviewBranches> listener) {
        myShowBranchesListeners.add(listener);
        Disposer.register(parentDisposable, () -> myShowBranchesListeners.remove(listener));
    }

    @Override
    public void fetchAndCheckoutRemoteBranch() {
        myCheckedOut.set(true);
    }

    @Override
    public boolean canShowInLog() {
        return true;
    }

    @Override
    public void fetchAndShowInLog() {
    }

    @Override
    public void showBranches() {
        CodeReviewBranches branches = new CodeReviewBranches(SOURCE, TARGET);
        for (Consumer<CodeReviewBranches> listener : myShowBranchesListeners) {
            listener.accept(branches);
        }
    }
}
