// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.event;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * This listener is always invoked in write action synchronously with the change of dumb mode status.
 * The thread of invocation is undefined.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface DumbModeListenerBackgroundable {
    @RequiredWriteAction
    default void enteredDumbMode() {
    }

    @RequiredWriteAction
    default void exitDumbMode() {
    }
}
