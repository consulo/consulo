// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.build.progress.BuildProgress;
import consulo.ide.impl.idea.build.progress.BuildProgressDescriptor;
import consulo.ide.impl.idea.build.progress.BuildRootProgressImpl;
import consulo.language.LangBundle;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
@Singleton
@Service(ComponentScope.PROJECT)
@ServiceImpl
public class BuildViewManager extends AbstractViewManager {
  @Inject
  public BuildViewManager(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Nonnull
  @Override
  public String getViewName() {
    return LangBundle.message("tab.title.build.output");
  }

  //@ApiStatus.Experimental
  public static BuildProgress<BuildProgressDescriptor> createBuildProgress(@Nonnull Project project) {
    return new BuildRootProgressImpl(ServiceManager.getService(project, BuildViewManager.class));
  }
}
