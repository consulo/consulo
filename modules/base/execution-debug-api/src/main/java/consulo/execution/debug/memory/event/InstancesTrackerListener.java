// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.memory.event;

import consulo.execution.debug.memory.TrackingType;

import java.util.EventListener;

public interface InstancesTrackerListener extends EventListener {
    default void classChanged(String name, TrackingType type) {
    }

    default void classRemoved(String name) {
    }

    default void backgroundTrackingValueChanged(boolean newState) {
    }
}
