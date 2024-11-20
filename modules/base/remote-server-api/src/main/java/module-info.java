/**
 * @author VISTALL
 * @since 22/01/2023
 */
module consulo.remote.server.api {
    // TODO remove in future
    requires java.desktop;

    requires transitive consulo.application.api;
    requires transitive consulo.project.api;
    requires transitive consulo.execution.api;
    requires transitive consulo.compiler.artifact.api;

    requires consulo.remote.servers.agent.rt;

    exports consulo.remoteServer;
    exports consulo.remoteServer.agent;
    exports consulo.remoteServer.configuration;
    exports consulo.remoteServer.configuration.deployment;
    exports consulo.remoteServer.runtime;
    exports consulo.remoteServer.runtime.deployment;
    exports consulo.remoteServer.runtime.deployment.debug;
    exports consulo.remoteServer.runtime.local;
    exports consulo.remoteServer.runtime.log;
    exports consulo.remoteServer.runtime.ui;
    exports consulo.remoteServer.localize;
}