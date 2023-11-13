// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildBundle;
import consulo.build.ui.BuildContentManager;
import consulo.build.ui.SyncViewManager;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.build.ui.impl.internal.BuildRootProgressImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
@Singleton
@ServiceImpl
public class SyncViewManagerImpl extends AbstractViewManager implements SyncViewManager {
  @Inject
  public SyncViewManagerImpl(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Nonnull
  @Override
  public String getViewName() {
    return BuildBundle.message("sync.view.title");
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> createBuildProgress() {
    return new BuildRootProgressImpl(this);
  }
}