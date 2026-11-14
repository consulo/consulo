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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.remoteServer.agent.impl.internal.nio.RemoteNioFileSystem;
import consulo.remoteServer.agent.impl.internal.platform.RemotePlatform;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.platformAware.PlatformAwareServerRuntimeInstance;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;

import io.grpc.Status;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runtime instance for a connected remote agent.
 * Holds the connection, platform, and filesystem objects.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
class RemoteAgentServerRuntimeInstance extends PlatformAwareServerRuntimeInstance<DeploymentConfiguration> {
    private final RemoteAgentConnection myConnection;
    private final RemotePlatform myPlatform;
    private final RemoteNioFileSystem myNioFileSystem;
    private final @Nullable AutoCloseable myLease;

    // the agent may die while connected (killed, its machine sleeps) - the calls fail then, the connection is lost
    private static final long PING_INTERVAL_SECONDS = 15;
    private static final long PING_TIMEOUT_SECONDS = 5;
    private static final int MAX_FAILED_PINGS = 2;

    private volatile @Nullable Consumer<LocalizeValue> myConnectionLostListener;
    private volatile @Nullable ScheduledFuture<?> myPing;
    private volatile boolean myDisconnected;
    private int myFailedPings;

    /**
     * @param lease the launched agent, stopped after the connection is closed; {@code null} for a configured agent
     */
    RemoteAgentServerRuntimeInstance(RemoteAgentConnection connection,
                                     RemotePlatform platform,
                                     RemoteNioFileSystem nioFileSystem,
                                     @Nullable AutoCloseable lease) {
        myConnection = connection;
        myPlatform = platform;
        myNioFileSystem = nioFileSystem;
        myLease = lease;
    }

    @Override
    public void deploy(DeploymentTask<DeploymentConfiguration> task,
                       DeploymentLogManager logManager,
                       DeploymentOperationCallback callback) {
        callback.errorOccurred(LocalizeValue.localizeTODO("Deployment is not supported for remote agent connections"));
    }

    @Override
    public void computeDeployments(ComputeDeploymentsCallback callback) {
        callback.succeeded();
    }

    @Override
    public void setConnectionLostListener(Consumer<LocalizeValue> listener) {
        myConnectionLostListener = listener;
        if (myPing == null) {
            myPing = AppExecutorUtil.getAppScheduledExecutorService()
                .scheduleWithFixedDelay(this::ping, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void ping() {
        if (myDisconnected) {
            return;
        }
        try {
            myConnection.ping(PING_TIMEOUT_SECONDS);
            myFailedPings = 0;
        }
        catch (RemoteAgentException e) {
            Status.Code code = e.getCode();
            if (code != Status.Code.UNAVAILABLE && code != Status.Code.DEADLINE_EXCEEDED) {
                // the agent answered - with an error of its own
                myFailedPings = 0;
                return;
            }
            if (++myFailedPings < MAX_FAILED_PINGS) {
                return;
            }
            stopPing();
            Consumer<LocalizeValue> listener = myConnectionLostListener;
            if (listener != null && !myDisconnected) {
                listener.accept(LocalizeValue.localizeTODO("Connection to the remote agent is lost: " + e.getMessage()));
            }
        }
    }

    private void stopPing() {
        ScheduledFuture<?> ping = myPing;
        if (ping != null) {
            ping.cancel(false);
        }
    }

    @Override
    public void disconnect() {
        myDisconnected = true;
        stopPing();
        // closed first: files of this platform become unavailable (not deleted) until it is connected again
        try {
            myNioFileSystem.close();
        }
        catch (IOException ignored) {
        }
        finally {
            try {
                myConnection.close();
            }
            finally {
                RemoteAgentServerConnector.closeLease(myLease);
            }
        }
    }

    public RemoteAgentConnection getConnection() {
        return myConnection;
    }

    @Override
    public Platform getPlatform() {
        return myPlatform;
    }

    public RemoteNioFileSystem getNioFileSystem() {
        return myNioFileSystem;
    }

}
