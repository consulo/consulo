// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildContentManager;
import consulo.build.ui.BuildViewManager;
import consulo.build.ui.impl.internal.BuildRootProgressImpl;
import consulo.build.ui.localize.BuildLocalize;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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

    @Override
    public String getViewId() {
        return "BuildOutput";
    }

    @Override
    public LocalizeValue getViewName() {
        return BuildLocalize.tabTitleBuildOutput();
    }

    @Override
    public BuildProgress<BuildProgressDescriptor> createBuildProgress() {
        BuildRootProgressImpl progress = new BuildRootProgressImpl();
        progress.addListener(this);
        return progress;
    }
}
