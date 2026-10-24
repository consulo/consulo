/**
 * @author VISTALL
 * @since 2026-10-13
 */
@SuppressWarnings("module")
module consulo.remote.server.agent.impl {
    requires consulo.remote.server.api;
    requires consulo.credential.storage.api;
    requires consulo.http.api;

    requires com.google.protobuf;
    requires io.grpc;
    requires io.grpc.netty;
    requires io.grpc.protobuf;
    requires io.grpc.stub;
    requires io.netty.handler;

    opens consulo.remoteServer.agent.impl.internal to consulo.util.xml.serializer;
    opens consulo.remoteServer.agent.impl.internal.protocol to com.google.protobuf;
}
