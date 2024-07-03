// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.dashboard;

import consulo.component.util.WeighedItem;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeValue;
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
  public static final RunDashboardRunConfigurationStatus STARTED =
    new RunDashboardRunConfigurationStatus(ExecutionLocalize.runDashboardStartedGroupName(), PlatformIconGroup.actionsExecute(), 10);
  public static final RunDashboardRunConfigurationStatus FAILED =
    new RunDashboardRunConfigurationStatus(ExecutionLocalize.runDashboardFailedGroupName(), PlatformIconGroup.generalError(), 20);
  public static final RunDashboardRunConfigurationStatus STOPPED =
    new RunDashboardRunConfigurationStatus(ExecutionLocalize.runDashboardStoppedGroupName(), PlatformIconGroup.actionsRestart(), 30);
  public static final RunDashboardRunConfigurationStatus CONFIGURED =
    new RunDashboardRunConfigurationStatus(ExecutionLocalize.runDashboardConfiguredGroupName(), PlatformIconGroup.generalSettings(), 40);

  private final LocalizeValue myName;
  private final Image myIcon;
  private final int myWeight;

  public RunDashboardRunConfigurationStatus(LocalizeValue name, Image icon, int weight) {
    myName = name;
    myIcon = icon;
    myWeight = weight;
  }

  public @Nls String getName() {
    return myName.get();
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
