// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.dashboard;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.ui.RunContentDescriptor;
import consulo.project.Project;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author konstantin.aleev
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface RunDashboardManager {
  @Deprecated
  Class<RunDashboardListener> DASHBOARD_TOPIC = RunDashboardListener.class;

  static RunDashboardManager getInstance(@Nonnull Project project) {
    return project.getInstance(RunDashboardManager.class);
  }

  ContentManager getDashboardContentManager();

  @Nonnull
  String getToolWindowId();

  @Nonnull
  Image getToolWindowIcon();

  void updateDashboard(boolean withStructure);

  List<RunDashboardService> getRunConfigurations();

  boolean isShowInDashboard(@Nonnull RunConfiguration runConfiguration);

  @Nonnull
  Set<String> getTypes();

  void setTypes(Set<String> types);

  @Nonnull
  Predicate<Content> getReuseCondition();

  interface RunDashboardService {
    @Nonnull
    RunnerAndConfigurationSettings getSettings();

    @Nullable
    RunContentDescriptor getDescriptor();

    @Nullable
    Content getContent();
  }
}
