package consulo.remoteServer.impl.internal.ui.tree;


public interface DeploymentNode extends ServersTreeNode {

    
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
