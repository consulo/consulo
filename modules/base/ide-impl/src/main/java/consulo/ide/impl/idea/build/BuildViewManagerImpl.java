// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildContentManager;
import consulo.build.ui.BuildViewManager;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.ide.impl.idea.build.progress.BuildRootProgressImpl;
import consulo.language.LangBundle;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
@Singleton
@ServiceImpl
public class BuildViewManagerImpl extends AbstractViewManager implements BuildViewManager {
  @Inject
  public BuildViewManagerImpl(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Nonnull
  @Override
  public String getViewName() {
    return LangBundle.message("tab.title.build.output");
  }

  @Nonnull
  @Override
  public BuildProgress<BuildProgressDescriptor> createBuildProgress() {
    return new BuildRootProgressImpl(this);
  }
}
