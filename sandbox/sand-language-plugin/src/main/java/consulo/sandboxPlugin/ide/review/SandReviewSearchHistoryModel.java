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

import consulo.review.list.search.PersistingReviewListSearchHistoryModel;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Keeps the sand searches in memory.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SandReviewSearchHistoryModel extends PersistingReviewListSearchHistoryModel<SandReviewSearchValue> {
    private volatile List<SandReviewSearchValue> myHistory = List.of();
    private volatile @Nullable SandReviewSearchValue myLastFilter;

    @Override
    protected List<SandReviewSearchValue> getPersistentHistory() {
        return myHistory;
    }

    @Override
    protected void setPersistentHistory(List<SandReviewSearchValue> history) {
        myHistory = history;
    }

    @Override
    public @Nullable SandReviewSearchValue getLastFilter() {
        return myLastFilter;
    }

    @Override
    public void setLastFilter(@Nullable SandReviewSearchValue lastFilter) {
        myLastFilter = lastFilter;
    }
}
