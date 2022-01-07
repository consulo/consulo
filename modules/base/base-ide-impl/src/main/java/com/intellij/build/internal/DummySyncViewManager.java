// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.internal;

import com.intellij.build.BuildContentManager;
import com.intellij.build.SyncViewManager;
import com.intellij.build.events.BuildEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
@TestOnly
public class DummySyncViewManager extends SyncViewManager {
  public DummySyncViewManager(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Override
  public void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {}
}
