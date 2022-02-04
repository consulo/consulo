package com.intellij.remoteServer.runtime.deployment;

import com.intellij.remoteServer.runtime.RemoteOperationCallback;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class DeploymentRuntime {
  public boolean isUndeploySupported() {
    return true;
  }

  public abstract void undeploy(@Nonnull UndeploymentTaskCallback callback);

  public interface UndeploymentTaskCallback extends RemoteOperationCallback {
    void succeeded();
  }
}
