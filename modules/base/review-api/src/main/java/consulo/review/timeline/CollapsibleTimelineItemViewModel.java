// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.timeline;

import consulo.util.concurrent.coroutine.ObservableValue;

public interface CollapsibleTimelineItemViewModel {
    ObservableValue<Boolean> getCollapsible();

    ObservableValue<Boolean> getCollapsed();

    void setCollapsed(boolean collapsed);
}
