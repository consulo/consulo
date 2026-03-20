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
package consulo.externalSystem.thrift.server;

import consulo.externalSystem.thrift.ExternalSystemFacadeService;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * Utility to start a Thrift server in the external system process.
 * <p>
 * The external process (e.g. Gradle worker) creates a handler implementing
 * {@link ExternalSystemFacadeService.Iface}, then calls {@link #start}
 * to begin serving. The port is printed to stdout so the IDE can connect.
 *
 * @author VISTALL
 * @since 2026-03-20
 */
public final class ThriftExternalSystemServer {

    private ThriftExternalSystemServer() {
    }

    /**
     * Starts a thread-pooled Thrift server on the given port using the provided executor.
     *
     * @param port            the port to listen on (use 0 for auto-assign)
     * @param handler         the service implementation
     * @param executorService the thread pool to use for serving requests
     * @return the running server (call {@link TServer#stop()} to shut down)
     */
    public static TServer start(int port, ExternalSystemFacadeService.Iface handler, ExecutorService executorService) {
        try {
            TServerSocket serverSocket = new TServerSocket(new InetSocketAddress("localhost", port));
            ExternalSystemFacadeService.Processor<ExternalSystemFacadeService.Iface> processor =
                new ExternalSystemFacadeService.Processor<>(handler);

            TThreadPoolServer server = new TThreadPoolServer(
                new TThreadPoolServer.Args(serverSocket)
                    .processor(processor)
                    .executorService(executorService)
            );

            Thread serverThread = new Thread(server::serve, "thrift-external-system-server");
            serverThread.setDaemon(true);
            serverThread.start();

            return server;
        }
        catch (TTransportException e) {
            throw new IllegalStateException("Failed to start Thrift server on port " + port, e);
        }
    }
}
