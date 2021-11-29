// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.build.progress.BuildRootProgressImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
@Singleton
public class SyncViewManager extends AbstractViewManager {
  @Inject
  public SyncViewManager(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Nonnull
  @Override
  public String getViewName() {
    return BuildBundle.message("sync.view.title");
  }

  // @ApiStatus.Experimental
  public static BuildProgress<BuildProgressDescriptor> createBuildProgress(@Nonnull Project project) {
    return new BuildRootProgressImpl(ServiceManager.getService(project, SyncViewManager.class));
  }
}