// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.ui.annotation.RequiredUIAccess;

@TopicAPI(ComponentScope.PROJECT)
public interface ChangesViewContentManagerListener {
    @RequiredUIAccess
    void toolWindowMappingChanged();
}
