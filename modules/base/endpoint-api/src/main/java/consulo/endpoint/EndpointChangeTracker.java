// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

@TopicAPI(ComponentScope.PROJECT)
public interface EndpointChangeTracker {
    void setTrackingChanges(boolean enabled);
}
