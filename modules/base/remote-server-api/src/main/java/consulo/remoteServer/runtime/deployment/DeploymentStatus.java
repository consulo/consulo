package consulo.remoteServer.runtime.deployment;

import jakarta.annotation.Nonnull;

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
