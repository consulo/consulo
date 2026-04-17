// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.timeline;

import consulo.collaboration.util.ReadOnlyObservableValue;

/**
 * View model for a collapsible timeline item.
 */
public interface CollapsibleTimelineItemViewModel {
    ReadOnlyObservableValue<Boolean> getCollapsible();

    ReadOnlyObservableValue<Boolean> getCollapsed();

    void setCollapsed(boolean collapsed);
}
