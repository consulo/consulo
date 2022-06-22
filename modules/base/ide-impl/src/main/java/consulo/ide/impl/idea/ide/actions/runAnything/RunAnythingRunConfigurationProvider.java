// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.execution.actions.ChooseRunConfigurationPopup;
import consulo.ide.IdeBundle;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil.fetchProject;

@ExtensionImpl
public class RunAnythingRunConfigurationProvider extends consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingRunConfigurationProvider {
  @Nonnull
  @Override
  public Collection<ChooseRunConfigurationPopup.ItemWrapper> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return getWrappers(dataContext);
  }

  @Nullable
  @Override
  public String getHelpGroupTitle() {
    return null;
  }

  @Nonnull
  @Override
  public String getCompletionGroupTitle() {
    return IdeBundle.message("run.anything.run.configurations.group.title");
  }

  @Nonnull
  private static List<ChooseRunConfigurationPopup.ItemWrapper> getWrappers(@Nonnull DataContext dataContext) {
    Project project = fetchProject(dataContext);
    return ChooseRunConfigurationPopup.createFlatSettingsList(project);
  }

  @Nonnull
  @Override
  public List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext) {
    return ContainerUtil.emptyList();
  }
}