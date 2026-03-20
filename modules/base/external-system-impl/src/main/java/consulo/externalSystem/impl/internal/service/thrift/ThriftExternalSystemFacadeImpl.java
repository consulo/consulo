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
package consulo.externalSystem.impl.internal.service.thrift;

import consulo.externalSystem.impl.internal.service.ExternalSystemCommunicationException;
import consulo.externalSystem.impl.internal.service.RemoteExternalSystemFacade;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProjectResolver;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemTaskManager;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.*;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.thrift.*;
import consulo.externalSystem.thrift.client.ThriftClientConnection;
import consulo.externalSystem.thrift.converter.ThriftTypeConverter;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Client-side Thrift implementation of {@link RemoteExternalSystemFacade}.
 * Connects to an external process running a Thrift server and delegates
 * all calls over the wire via {@link ThriftClientConnection}.
 *
 * @author VISTALL
 * @since 2026-03-20
 */
public class ThriftExternalSystemFacadeImpl implements RemoteExternalSystemFacade<ExternalSystemExecutionSettings> {

    private final ThriftClientConnection myConnection;

    private final ThriftProjectResolverProxy myResolver;
    private final ThriftTaskManagerProxy myTaskManager;

    public ThriftExternalSystemFacadeImpl(String host, int port) {
        try {
            myConnection = new ThriftClientConnection(host, port);
        }
        catch (Exception e) {
            throw new ExternalSystemCommunicationException("Failed to connect to external system process at " + host + ":" + port, e);
        }
        myResolver = new ThriftProjectResolverProxy();
        myTaskManager = new ThriftTaskManagerProxy();
    }

    public boolean isConnected() {
        return myConnection.isOpen();
    }

    public void close() {
        myConnection.close();
    }

    /**
     * Polls progress events from the remote process and dispatches them to the given listener.
     */
    public void pollAndDispatchProgress(ExternalSystemTaskNotificationListener listener) {
        try {
            List<ThriftProgressEvent> events = myConnection.pollProgressEvents();
            for (ThriftProgressEvent event : events) {
                ExternalSystemTaskId taskId = ThriftTypeConverter.fromThrift(event.getTaskId());
                switch (event.getType()) {
                    case QUEUED:
                        listener.onQueued(taskId);
                        break;
                    case START:
                        listener.onStart(taskId);
                        break;
                    case STATUS_CHANGE:
                        listener.onStatusChange(new ExternalSystemTaskNotificationEvent(taskId, event.getDescription()));
                        break;
                    case TASK_OUTPUT:
                        listener.onTaskOutput(taskId, event.getOutput(), event.isStdOut());
                        break;
                    case END:
                        listener.onEnd(taskId);
                        break;
                    case SUCCESS:
                        listener.onSuccess(taskId);
                        break;
                    case FAILURE:
                        Exception ex = event.isSetFailure()
                            ? ThriftTypeConverter.fromThrift(event.getFailure())
                            : new RuntimeException(event.getDescription());
                        listener.onFailure(taskId, ex);
                        break;
                }
            }
        }
        catch (Exception e) {
            throw new ExternalSystemCommunicationException("Failed to poll progress events", e);
        }
    }

    @Override
    public RemoteExternalSystemProjectResolver<ExternalSystemExecutionSettings> getResolver() {
        return myResolver;
    }

    @Override
    public RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings> getTaskManager() {
        return myTaskManager;
    }

    @Override
    public void applySettings(ExternalSystemExecutionSettings settings) {
        try {
            ThriftExecutionSettings thriftSettings = ThriftTypeConverter.toThrift(settings, Collections.emptyMap());
            myConnection.applySettings(thriftSettings);
        }
        catch (Exception e) {
            throw new ExternalSystemCommunicationException("Failed to apply settings", e);
        }
    }

    @Override
    public void applyProgressManager(RemoteExternalSystemProgressNotificationManager progressManager) {
        // Progress is handled via polling, not callbacks.
        // The ThriftExternalSystemCommunicationManager sets up the polling loop.
    }

    @Override
    public boolean isTaskInProgress(ExternalSystemTaskId id) {
        try {
            ThriftTaskId thriftId = ThriftTypeConverter.toThrift(id);
            return myConnection.isTaskInProgress(thriftId);
        }
        catch (Exception e) {
            throw new ExternalSystemCommunicationException(e);
        }
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId id) {
        try {
            ThriftTaskId thriftId = ThriftTypeConverter.toThrift(id);
            return myConnection.cancelTask(thriftId);
        }
        catch (Exception e) {
            throw new ExternalSystemCommunicationException(e);
        }
    }

    @Override
    public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
        return Collections.emptyMap();
    }

    // ============ Inner proxy classes ============

    private class ThriftProjectResolverProxy implements RemoteExternalSystemProjectResolver<ExternalSystemExecutionSettings> {

        @Nullable
        @Override
        public DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
                                                        String projectPath,
                                                        boolean isPreviewMode,
                                                        @Nullable ExternalSystemExecutionSettings settings)
            throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
            try {
                ThriftTaskId thriftId = ThriftTypeConverter.toThrift(id);
                ThriftExecutionSettings thriftSettings = settings != null
                    ? ThriftTypeConverter.toThrift(settings, Collections.emptyMap())
                    : new ThriftExecutionSettings(0, false, Collections.emptyMap());

                ThriftDataNode result = myConnection.resolveProjectInfo(thriftId, projectPath, isPreviewMode, thriftSettings);
                return ThriftTypeConverter.fromThrift(result);
            }
            catch (ThriftExternalSystemException e) {
                throw ThriftTypeConverter.fromThrift(e);
            }
            catch (Exception e) {
                throw new ExternalSystemCommunicationException("Failed to resolve project info", e);
            }
        }

        @Override
        public void setSettings(ExternalSystemExecutionSettings settings) {
        }

        @Override
        public void setNotificationListener(ExternalSystemTaskNotificationListener notificationListener) {
        }

        @Override
        public boolean isTaskInProgress(ExternalSystemTaskId id) {
            return ThriftExternalSystemFacadeImpl.this.isTaskInProgress(id);
        }

        @Override
        public boolean cancelTask(ExternalSystemTaskId id) {
            return ThriftExternalSystemFacadeImpl.this.cancelTask(id);
        }

        @Override
        public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
            return Collections.emptyMap();
        }
    }

    private class ThriftTaskManagerProxy implements RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings> {

        @Override
        public void executeTasks(ExternalSystemTaskId id,
                                 List<String> taskNames,
                                 String projectPath,
                                 @Nullable ExternalSystemExecutionSettings settings,
                                 List<String> vmOptions,
                                 List<String> scriptParameters,
                                 @Nullable String debuggerSetup) throws ExternalSystemException {
            try {
                ThriftTaskId thriftId = ThriftTypeConverter.toThrift(id);
                ThriftExecutionSettings thriftSettings = settings != null
                    ? ThriftTypeConverter.toThrift(settings, Collections.emptyMap())
                    : new ThriftExecutionSettings(0, false, Collections.emptyMap());

                myConnection.executeTasks(
                    thriftId,
                    taskNames,
                    projectPath,
                    thriftSettings,
                    vmOptions != null ? vmOptions : Collections.emptyList(),
                    scriptParameters != null ? scriptParameters : Collections.emptyList(),
                    debuggerSetup != null ? debuggerSetup : ""
                );
            }
            catch (ThriftExternalSystemException e) {
                throw ThriftTypeConverter.fromThrift(e);
            }
            catch (Exception e) {
                throw new ExternalSystemCommunicationException("Failed to execute tasks", e);
            }
        }

        @Override
        public boolean cancelTask(ExternalSystemTaskId id) throws ExternalSystemException {
            return ThriftExternalSystemFacadeImpl.this.cancelTask(id);
        }

        @Override
        public void setSettings(ExternalSystemExecutionSettings settings) {
        }

        @Override
        public void setNotificationListener(ExternalSystemTaskNotificationListener notificationListener) {
        }

        @Override
        public boolean isTaskInProgress(ExternalSystemTaskId id) {
            return ThriftExternalSystemFacadeImpl.this.isTaskInProgress(id);
        }

        @Override
        public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
            return Collections.emptyMap();
        }
    }
}
