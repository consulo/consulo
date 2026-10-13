/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.remoteServer.agent.impl.internal;

import com.google.protobuf.ByteString;
import consulo.remoteServer.agent.impl.internal.protocol.*;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RemoteAgentConnection implements AutoCloseable {
    private static final int MAX_MESSAGE_SIZE = 64 * 1024 * 1024;
    private static final long CONNECT_TIMEOUT_SECONDS = 10;
    private static final Metadata.Key<String> PERMISSION_KEY = Metadata.Key.of("permission", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> AUTHORIZATION_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final String myHost;
    private final int myPort;
    private final boolean myUseTls;
    private final String myCertificateFingerprint;
    private final @Nullable String myToken;
    private final Executor myExecutor;

    private volatile @Nullable ManagedChannel myChannel;

    public RemoteAgentConnection(String host,
                                 int port,
                                 boolean useTls,
                                 String certificateFingerprint,
                                 @Nullable String token,
                                 Executor executor) {
        myHost = host;
        myPort = port;
        myUseTls = useTls;
        myCertificateFingerprint = certificateFingerprint;
        myToken = token;
        myExecutor = executor;
    }

    public synchronized void connect() {
        if (isConnected()) {
            return;
        }

        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(myHost, myPort)
            .executor(myExecutor)
            .maxInboundMessageSize(MAX_MESSAGE_SIZE);
        if (myUseTls) {
            builder.sslContext(createSslContext());
        }
        else {
            builder.usePlaintext();
        }
        if (myToken != null && !myToken.isEmpty()) {
            Metadata headers = new Metadata();
            headers.put(AUTHORIZATION_KEY, "Bearer " + myToken);
            builder.intercept(MetadataUtils.newAttachHeadersInterceptor(headers));
        }

        ManagedChannel channel = builder.build();
        try {
            RemoteAgentServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getAgentInfo(Empty.getDefaultInstance());
        }
        catch (StatusRuntimeException e) {
            channel.shutdownNow();
            throw new RemoteAgentException("Failed to connect to remote agent at " + address() + ": " + describe(e), e.getStatus().getCode(), e);
        }
        myChannel = channel;
    }

    private SslContext createSslContext() {
        try {
            SslContextBuilder builder = GrpcSslContexts.forClient();
            if (!myCertificateFingerprint.isEmpty()) {
                builder.trustManager(new RemoteAgentTrustManager(myCertificateFingerprint));
            }
            return builder.build();
        }
        catch (SSLException e) {
            throw new RemoteAgentException("Failed to set up TLS for remote agent at " + address() + ": " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        ManagedChannel channel = myChannel;
        return channel != null && channel.getState(false) != ConnectivityState.SHUTDOWN;
    }

    public AgentInfo getAgentInfo() {
        return call(stub -> stub.getAgentInfo(Empty.getDefaultInstance()));
    }

    public String getWorkspacePath() {
        return call(stub -> stub.getWorkspacePath(Empty.getDefaultInstance())).getPath();
    }

    public SystemInfo getSystemInfo() {
        return call(stub -> stub.getSystemInfo(Empty.getDefaultInstance()));
    }

    public UserInfo getUserInfo() {
        return call(stub -> stub.getUserInfo(Empty.getDefaultInstance()));
    }

    public Map<String, String> getEnvVariables() {
        return call(stub -> stub.getEnvVariables(Empty.getDefaultInstance())).getVariablesMap();
    }

    public List<ProcessInfo> listProcesses() {
        return call(stub -> stub.listProcesses(Empty.getDefaultInstance())).getProcessesList();
    }

    public boolean killProcess(long pid, boolean force) {
        return call(stub -> stub.killProcess(KillRequest.newBuilder().setPid(pid).setForce(force).build())).getKilled();
    }

    public StreamObserver<ExecInput> exec(StreamObserver<ExecEvent> events) {
        return RemoteAgentServiceGrpc.newStub(channel()).exec(events);
    }

    public byte[] readFile(String path) {
        return call(stub -> stub.readFile(pathRequest(path))).getData().toByteArray();
    }

    public void writeFile(String path, byte[] data) {
        call(stub -> stub.writeFile(WriteFileRequest.newBuilder().setPath(path).setData(ByteString.copyFrom(data)).build()));
    }

    public boolean deleteFile(String path) {
        return call(stub -> stub.deleteFile(pathRequest(path))).getDeleted();
    }

    public List<FileInfo> listDirectory(String path) {
        return call(stub -> stub.listDirectory(pathRequest(path))).getFilesList();
    }

    public boolean fileExists(String path) {
        return call(stub -> stub.fileExists(pathRequest(path))).getExists();
    }

    public void createDirectory(String path, boolean recursive) {
        call(stub -> stub.createDirectory(CreateDirectoryRequest.newBuilder().setPath(path).setRecursive(recursive).build()));
    }

    public List<FileInfo> listRoots() {
        return call(stub -> stub.listRoots(Empty.getDefaultInstance())).getFilesList();
    }

    public boolean setPermissions(String path, int mode) {
        return call(stub -> stub.setPermissions(SetPermissionsRequest.newBuilder().setPath(path).setMode(mode).build())).getChanged();
    }

    public HttpResponse executeHttpRequest(HttpRequest request) {
        return call(stub -> stub.executeHttpRequest(request));
    }

    public String getHost() {
        return myHost;
    }

    public int getPort() {
        return myPort;
    }

    /**
     * Names the machine for as long as its address stays the same - the agent's own id is the same for every agent.
     */
    public String getPlatformId() {
        return "remote-agent@" + myHost + ":" + myPort;
    }

    public RemoteAgentException toException(Throwable error) {
        if (error instanceof StatusRuntimeException statusError) {
            Status status = statusError.getStatus();
            if (status.getCode() == Status.Code.PERMISSION_DENIED) {
                Metadata trailers = statusError.getTrailers();
                String permission = trailers == null ? null : trailers.get(PERMISSION_KEY);
                return new RemotePermissionException(describe(statusError), permission == null ? "" : permission, statusError);
            }
            if (status.getCode() == Status.Code.UNAVAILABLE) {
                return new RemoteAgentException("Connection lost to remote agent at " + address(), status.getCode(), statusError);
            }
            return new RemoteAgentException(describe(statusError), status.getCode(), statusError);
        }
        if (error instanceof RemoteAgentException agentException) {
            return agentException;
        }
        return new RemoteAgentException("Unexpected error: " + error.getMessage(), error);
    }

    @Override
    public synchronized void close() {
        ManagedChannel channel = myChannel;
        if (channel != null) {
            myChannel = null;
            channel.shutdownNow();
        }
    }

    private <T> T call(Function<RemoteAgentServiceGrpc.RemoteAgentServiceBlockingStub, T> call) {
        try {
            return call.apply(RemoteAgentServiceGrpc.newBlockingStub(channel()));
        }
        catch (StatusRuntimeException e) {
            throw toException(e);
        }
    }

    private ManagedChannel channel() {
        ManagedChannel channel = myChannel;
        if (channel == null) {
            throw new RemoteAgentException("Not connected to remote agent at " + address(), Status.Code.UNAVAILABLE, null);
        }
        return channel;
    }

    private String address() {
        return myHost + ":" + myPort;
    }

    private static PathRequest pathRequest(String path) {
        return PathRequest.newBuilder().setPath(path).build();
    }

    private static String describe(StatusRuntimeException e) {
        Status status = e.getStatus();
        if (status.getCode() == Status.Code.UNAUTHENTICATED) {
            return "the agent rejected the token";
        }
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof CertificateException) {
                return "TLS certificate rejected: " + cause.getMessage();
            }
            if (cause instanceof SSLException) {
                return "TLS handshake failed: " + cause.getMessage();
            }
        }
        String description = status.getDescription();
        return description != null ? description : status.getCode().name();
    }
}
