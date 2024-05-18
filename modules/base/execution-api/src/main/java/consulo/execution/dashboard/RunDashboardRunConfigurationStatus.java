// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.dashboard;

import consulo.component.util.WeighedItem;
import consulo.execution.ExecutionBundle;
import consulo.execution.ui.RunContentDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.BaseProcessHandler;
import consulo.process.ProcessHandler;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

/**
 * @author konstantin.aleev
 */
public class RunDashboardRunConfigurationStatus implements WeighedItem {
  public static final RunDashboardRunConfigurationStatus STARTED = new RunDashboardRunConfigurationStatus(
    ExecutionBundle.message("run.dashboard.started.group.name"), PlatformIconGroup.actionsExecute(), 10);
  public static final RunDashboardRunConfigurationStatus FAILED = new RunDashboardRunConfigurationStatus(
    ExecutionBundle.message("run.dashboard.failed.group.name"), PlatformIconGroup.generalError(), 20);
  public static final RunDashboardRunConfigurationStatus STOPPED = new RunDashboardRunConfigurationStatus(
    ExecutionBundle.message("run.dashboard.stopped.group.name"), PlatformIconGroup.actionsRestart(), 30);
  public static final RunDashboardRunConfigurationStatus CONFIGURED = new RunDashboardRunConfigurationStatus(
    ExecutionBundle.message("run.dashboard.configured.group.name"), PlatformIconGroup.generalSettings(), 40);

  private final @Nls String myName;
  private final Image myIcon;
  private final int myWeight;

  public RunDashboardRunConfigurationStatus(@Nls String name, Image icon, int weight) {
    myName = name;
    myIcon = icon;
    myWeight = weight;
  }

  public @Nls String getName() {
    return myName;
  }

  public Image getIcon() {
    return myIcon;
  }

  @Override
  public int getWeight() {
    return myWeight;
  }

  public static @Nonnull RunDashboardRunConfigurationStatus getStatus(RunDashboardRunConfigurationNode node) {
    RunContentDescriptor descriptor = node.getDescriptor();
    if (descriptor == null) {
      return CONFIGURED;
    }
    ProcessHandler processHandler = descriptor.getProcessHandler();
    if (processHandler == null) {
      return STOPPED;
    }
    Integer exitCode = processHandler.getExitCode();
    if (exitCode == null) {
      return STARTED;
    }
    Boolean terminationRequested = processHandler.getUserData(BaseProcessHandler.TERMINATION_REQUESTED);
    if (exitCode == 0 || (terminationRequested != null && terminationRequested)) {
      return STOPPED;
    }
    return FAILED;
  }
}
