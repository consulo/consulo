// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.timeline;

import kotlinx.coroutines.flow.Flow;

public interface CollapsibleTimelineItemViewModel {
    Flow<Boolean> getCollapsible();

    Flow<Boolean> getCollapsed();

    void setCollapsed(boolean collapsed);
}
