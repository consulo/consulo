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
package consulo.externalSystem.thrift.client;

import consulo.externalSystem.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.List;

/**
 * Wraps a Thrift client connection to an external system process.
 * Hides Thrift transport details from the consumer.
 *
 * @author VISTALL
 * @since 2026-03-20
 */
public final class ThriftClientConnection implements AutoCloseable {

    private final TTransport myTransport;
    private final ExternalSystemFacadeService.Client myClient;

    public ThriftClientConnection(String host, int port) throws TException {
        myTransport = new TSocket(host, port);
        myTransport.open();
        myClient = new ExternalSystemFacadeService.Client(new TBinaryProtocol(myTransport));
    }

    public boolean isOpen() {
        return myTransport.isOpen();
    }

    @Override
    public void close() {
        myTransport.close();
    }

    public synchronized ThriftDataNode resolveProjectInfo(ThriftTaskId id,
                                                          String projectPath,
                                                          boolean isPreviewMode,
                                                          ThriftExecutionSettings settings)
        throws ThriftExternalSystemException, TException {
        return myClient.resolveProjectInfo(id, projectPath, isPreviewMode, settings);
    }

    public synchronized void executeTasks(ThriftTaskId id,
                                          List<String> taskNames,
                                          String projectPath,
                                          ThriftExecutionSettings settings,
                                          List<String> vmOptions,
                                          List<String> scriptParameters,
                                          String debuggerSetup)
        throws ThriftExternalSystemException, TException {
        myClient.executeTasks(id, taskNames, projectPath, settings, vmOptions, scriptParameters, debuggerSetup);
    }

    public synchronized boolean cancelTask(ThriftTaskId id) throws TException {
        return myClient.cancelTask(id);
    }

    public synchronized boolean isTaskInProgress(ThriftTaskId id) throws TException {
        return myClient.isTaskInProgress(id);
    }

    public synchronized void applySettings(ThriftExecutionSettings settings) throws TException {
        myClient.applySettings(settings);
    }

    public synchronized List<ThriftProgressEvent> pollProgressEvents() throws TException {
        return myClient.pollProgressEvents();
    }
}
