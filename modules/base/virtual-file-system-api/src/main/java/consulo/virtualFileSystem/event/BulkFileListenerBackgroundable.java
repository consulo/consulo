// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import java.util.List;

/**
 * A listener for VFS events, invoked inside write-action.
 * <p>
 * This listener is always invoked on a background thread under write action.
 * It is not guaranteed that {@link #before} and {@link #after} will be invoked on the same thread.
 * <p>
 * Please use {@link AsyncFileListener} instead, unless you absolutely sure you need to receive events synchronously.
 * <p>
 * Please note that the VFS events are project-agnostic so all listeners will be notified about events from all open
 * projects.
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface BulkFileListenerBackgroundable extends BulkFileListenerBase {
    @Override
    default void before(List<? extends VFileEvent> events) {
    }

    @Override
    default void after(List<? extends VFileEvent> events) {
    }
}
