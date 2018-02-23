package com.intellij.remoteServer.impl.runtime;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class DeployToServerRunner extends DefaultProgramRunner {
  @Nonnull
  @Override
  public String getRunnerId() {
    return "DeployToServer";
  }

  @Override
  public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
    if (!(profile instanceof DeployToServerRunConfiguration)) {
      return false;
    }
    if (executorId.equals(DefaultRunExecutor.EXECUTOR_ID)) {
      return true;
    }
    if (executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)) {
      return ((DeployToServerRunConfiguration<?, ?>)profile).getServerType().createDebugConnector() != null;
    }
    return false;
  }
}
