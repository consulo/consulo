// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.event;

import java.util.List;

public interface BulkFileListenerBase {
    default void before(List<? extends VFileEvent> events) {
    }

    default void after(List<? extends VFileEvent> events) {
    }
}
