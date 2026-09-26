// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

@TopicAPI(ComponentScope.PROJECT)
public interface ExternalSystemProjectNotificationAwareListener {
    /**
     * Happens when notification should be shown or hidden.
     */
    default void onNotificationChanged() {
    }
}
