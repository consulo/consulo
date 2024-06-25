// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.versionControlSystem.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.EventListener;

@TopicAPI(ComponentScope.PROJECT)
public interface ChangeListAvailabilityListener extends EventListener {
  Class<ChangeListAvailabilityListener> TOPIC = ChangeListAvailabilityListener.class;

  @RequiredUIAccess
  default void onBefore() {
  }

  @RequiredUIAccess
  default void onAfter() {
  }
}
