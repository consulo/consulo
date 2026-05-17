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
package consulo.enviroment.remoteAgent;

import consulo.enviroment.remoteAgent.protocol.AgentException;
import consulo.enviroment.remoteAgent.protocol.PermissionException;
import consulo.enviroment.remoteAgent.protocol.RemoteAgentService;
import consulo.logging.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe wrapper around a Thrift connection to the remote agent.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteAgentConnection implements AutoCloseable {
    private static final Logger LOG = Logger.getInstance(RemoteAgentConnection.class);

    private final String myHost;
    private final int myPort;
    private final ReentrantLock myLock = new ReentrantLock();

    private TSocket mySocket;
    private RemoteAgentService.Client myClient;
    private String myPlatformId;

    public RemoteAgentConnection(String host, int port) {
        myHost = host;
        myPort = port;
    }

    public void connect() {
        myLock.lock();
        try {
            if (mySocket != null && mySocket.isOpen()) {
                return;
            }

            mySocket = new TSocket(myHost, myPort);
            mySocket.open();
            TBinaryProtocol protocol = new TBinaryProtocol(mySocket);
            myClient = new RemoteAgentService.Client(protocol);
            LOG.info("Connected to remote agent at " + myHost + ":" + myPort);
        }
        catch (TTransportException e) {
            throw new RemoteAgentException("Failed to connect to remote agent at " + myHost + ":" + myPort, e);
        }
        finally {
            myLock.unlock();
        }
    }

    public <T> T execute(ThriftCall<T> call) {
        myLock.lock();
        try {
            if (myClient == null) {
                throw new RemoteAgentException("Not connected to remote agent");
            }
            return call.call(myClient);
        }
        catch (RemoteAgentException e) {
            throw e;
        }
        catch (AgentException e) {
            throw new RemoteAgentException(e.getMessage(), e);
        }
        catch (PermissionException e) {
            throw new RemotePermissionException(e.getMessage(), e.getPermission());
        }
        catch (TTransportException e) {
            throw new RemoteAgentException("Connection lost to remote agent at " + myHost + ":" + myPort, e);
        }
        catch (TException e) {
            throw new RemoteAgentException("Thrift protocol error: " + e.getMessage(), e);
        }
        catch (Exception e) {
            throw new RemoteAgentException("Unexpected error: " + e.getMessage(), e);
        }
        finally {
            myLock.unlock();
        }
    }

    public void executeVoid(ThriftVoidCall call) {
        execute(client -> {
            call.call(client);
            return null;
        });
    }

    public boolean isConnected() {
        myLock.lock();
        try {
            return mySocket != null && mySocket.isOpen();
        }
        finally {
            myLock.unlock();
        }
    }

    public String getHost() {
        return myHost;
    }

    public int getPort() {
        return myPort;
    }

    public String getPlatformId() {
        return myPlatformId;
    }

    public void setPlatformId(String platformId) {
        myPlatformId = platformId;
    }

    @Override
    public void close() {
        myLock.lock();
        try {
            if (mySocket != null) {
                mySocket.close();
                mySocket = null;
                myClient = null;
                LOG.info("Disconnected from remote agent at " + myHost + ":" + myPort);
            }
        }
        finally {
            myLock.unlock();
        }
    }
}
