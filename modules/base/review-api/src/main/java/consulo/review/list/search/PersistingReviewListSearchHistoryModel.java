// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list.search;

import java.util.ArrayList;
import java.util.List;

public abstract class PersistingReviewListSearchHistoryModel<S extends ReviewListSearchValue> implements ReviewListSearchHistoryModel<S> {
    private final int myHistorySizeLimit;

    protected PersistingReviewListSearchHistoryModel() {
        this(10);
    }

    protected PersistingReviewListSearchHistoryModel(int historySizeLimit) {
        myHistorySizeLimit = historySizeLimit;
    }

    @Override
    public List<S> getHistory() {
        return getPersistentHistory();
    }

    protected abstract List<S> getPersistentHistory();

    protected abstract void setPersistentHistory(List<S> history);

    @Override
    public void add(S search) {
        List<S> history = new ArrayList<>(getPersistentHistory());
        history.remove(search);
        history.add(search);
        int from = Math.max(0, history.size() - myHistorySizeLimit);
        setPersistentHistory(List.copyOf(history.subList(from, history.size())));
    }
}
