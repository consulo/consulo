// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.memory.event;

import consulo.execution.debug.memory.MemoryViewManagerState;
import jakarta.annotation.Nonnull;

import java.util.EventListener;

@FunctionalInterface
public interface MemoryViewManagerListener extends EventListener {
    void stateChanged(@Nonnull MemoryViewManagerState state);
}
