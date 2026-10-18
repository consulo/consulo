// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.versionControlSystem.review;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeDetails;
import consulo.versionControlSystem.review.details.model.CodeReviewDetailedChangeListViewModel;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class CodeReviewProgressTreeModelFromDetails extends CodeReviewProgressTreeModel<RefComparisonChange> {
    private final ObservableValue<Map<RefComparisonChange, CodeReviewChangeDetails>> myDetails;
    private final ObservableValue<Boolean> myIsLoading;

    public CodeReviewProgressTreeModelFromDetails(Disposable cs, CodeReviewDetailedChangeListViewModel vm) {
        myDetails = vm.getDetailsByChange();
        myIsLoading = ObservableValue.of(false);
        Runnable unsubscribe = myDetails.addListener(details -> fireModelChanged());
        Disposer.register(cs, unsubscribe::run);
    }

    @Override
    public ObservableValue<Boolean> isLoading() {
        return myIsLoading;
    }

    @Override
    public @Nullable RefComparisonChange asLeaf(RefComparisonChange change) {
        return change;
    }

    @Override
    public boolean isLoading(RefComparisonChange leafValue) {
        return myDetails.get().get(leafValue) == null;
    }

    @Override
    public boolean isRead(RefComparisonChange leafValue) {
        CodeReviewChangeDetails details = myDetails.get().get(leafValue);
        return details == null || details.isRead();
    }

    @Override
    public int getUnresolvedDiscussionsCount(RefComparisonChange leafValue) {
        CodeReviewChangeDetails details = myDetails.get().get(leafValue);
        return details == null ? 0 : details.discussions();
    }
}
