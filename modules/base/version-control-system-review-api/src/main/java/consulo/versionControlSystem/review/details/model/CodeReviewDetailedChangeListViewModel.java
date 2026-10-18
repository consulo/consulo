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

import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.RefComparisonChange;

import java.util.Map;

public interface CodeReviewDetailedChangeListViewModel extends CodeReviewChangeListViewModel {
    /**
     * Map of additional details for changes
     */
    ObservableValue<Map<RefComparisonChange, CodeReviewChangeDetails>> getDetailsByChange();

    default boolean isViewedStateForAllChanges(Iterable<RefComparisonChange> changes, boolean viewed) {
        Map<RefComparisonChange, CodeReviewChangeDetails> details = getDetailsByChange().get();
        for (RefComparisonChange change : changes) {
            CodeReviewChangeDetails changeDetails = details.get(change);
            if (changeDetails == null || changeDetails.isRead() != viewed) {
                return false;
            }
        }
        return true;
    }
}
