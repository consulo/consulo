package consulo.remoteServer.impl.internal.ui.tree;

import jakarta.annotation.Nonnull;

public interface DeploymentNode extends ServersTreeNode {

    @Nonnull
    ServerNode getServerNode();

    boolean isDeployActionVisible();

    boolean isDeployActionEnabled();

    void deploy();

    boolean isUndeployActionEnabled();

    void undeploy();

    boolean isDebugActionVisible();

    void deployWithDebug();

    boolean isDeployed();

    String getDeploymentName();
}
