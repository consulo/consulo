package com.intellij.remoteServer.runtime.deployment;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public enum DeploymentStatus {
  DEPLOYED("Deployed"), NOT_DEPLOYED("Not deployed"), DEPLOYING("Deploying"), UNDEPLOYING("Undeploying");
  private String myPresentableText;

  DeploymentStatus(@Nonnull String presentableText) {
    myPresentableText = presentableText;
  }

  public String getPresentableText() {
    return myPresentableText;
  }
}
