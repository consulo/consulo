/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.util.socketConnection.impl;

import consulo.util.collection.Lists;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.socketConnection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author nik
 */
public abstract class SocketConnectionBase<Request extends AbstractRequest, Response extends AbstractResponse> implements SocketConnection<Request, Response> {
  private static final Logger LOG = LoggerFactory.getLogger(SocketConnectionBase.class);
  private final Object myLock = new Object();
  private int myPort = -1;
  private final AtomicReference<ConnectionState> myState = new AtomicReference<>(new ConnectionState(ConnectionStatus.NOT_CONNECTED));
  private boolean myStopping;
  private final List<SocketConnectionListener> myDispatcher = Lists.newLockFreeCopyOnWriteList();
  private List<Thread> myThreadsToInterrupt = new ArrayList<>();
  private final RequestResponseExternalizerFactory<Request, Response> myExternalizerFactory;
  private final LinkedBlockingQueue<Request> myRequests = new LinkedBlockingQueue<>();
  private final IntObjectMap<TimeoutInfo> myTimeouts = IntMaps.newIntObjectHashMap();
  private final ResponseProcessor<Response> myResponseProcessor;

  public SocketConnectionBase(@Nonnull ScheduledExecutorService executor, @Nonnull RequestResponseExternalizerFactory<Request, Response> factory) {
    myResponseProcessor = new ResponseProcessor<>(executor, this);
    myExternalizerFactory = factory;
  }

  @Override
  public void sendRequest(@Nonnull Request request) {
    sendRequest(request, null);
  }

  @Override
  public void sendRequest(@Nonnull Request request, @Nullable AbstractResponseToRequestHandler<? extends Response> handler) {
    if (handler != null) {
      myResponseProcessor.registerHandler(request.getId(), handler);
    }

    try {
      myRequests.put(request);
    }
    catch (InterruptedException ignored) {
    }
  }

  @Override
  public void sendRequest(@Nonnull Request request, @Nullable AbstractResponseToRequestHandler<? extends Response> handler, int timeout, @Nonnull Runnable onTimeout) {
    myTimeouts.put(request.getId(), new TimeoutInfo(timeout, onTimeout));
    sendRequest(request, handler);
  }

  @Override
  public <R extends Response> void registerHandler(@Nonnull Class<R> responseClass, @Nonnull AbstractResponseHandler<R> handler) {
    myResponseProcessor.registerHandler(responseClass, handler);
  }

  @Override
  public boolean isStopping() {
    synchronized (myLock) {
      return myStopping;
    }
  }

  protected void processRequests(RequestWriter<Request> writer) throws IOException {
    addThreadToInterrupt();
    try {
      while (!isStopping()) {
        final Request request = myRequests.take();
        LOG.debug("sending request: " + request);
        final TimeoutInfo timeoutInfo = myTimeouts.remove(request.getId());
        if (timeoutInfo != null) {
          myResponseProcessor.registerTimeoutHandler(request.getId(), timeoutInfo.myTimeout, timeoutInfo.myOnTimeout);
        }
        writer.writeRequest(request);
      }
    }
    catch (InterruptedException ignored) {
    }
    setStatus(ConnectionStatus.DISCONNECTED, null);
    removeThreadToInterrupt();
  }

  protected void addThreadToInterrupt() {
    synchronized (myLock) {
      myThreadsToInterrupt.add(Thread.currentThread());
    }
  }

  protected void removeThreadToInterrupt() {
    synchronized (myLock) {
      myThreadsToInterrupt.remove(Thread.currentThread());
    }
  }

  @Override
  public int getPort() {
    return myPort;
  }

  protected void setStatus(@Nonnull ConnectionStatus status, @Nullable String message) {
    synchronized (myLock) {
      myState.set(new ConnectionState(status, message, null));
    }

    for (SocketConnectionListener listener : myDispatcher) {
      try {
        listener.statusChanged(status);
      }
      catch (Throwable e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  @Override
  @Nonnull
  public ConnectionState getState() {
    synchronized (myLock) {
      return myState.get();
    }
  }

  @Nonnull
  @Override
  public Runnable addListener(@Nonnull SocketConnectionListener listener) {
    myDispatcher.add(listener);
    return () -> myDispatcher.remove(listener);
  }

  @Override
  public void close() {
    synchronized (myLock) {
      if (myStopping) return;
      myStopping = true;
    }
    LOG.debug("closing connection");
    synchronized (myLock) {
      for (Thread thread : myThreadsToInterrupt) {
        thread.interrupt();
      }
    }
    onClosing();
    myResponseProcessor.stopReading();
  }

  protected void onClosing() {
  }

  protected void attachToSocket(Socket socket) throws IOException {
    setStatus(ConnectionStatus.CONNECTED, null);
    LOG.debug("connected");

    final OutputStream outputStream = socket.getOutputStream();
    final InputStream inputStream = socket.getInputStream();
    myResponseProcessor.startReading(myExternalizerFactory.createResponseReader(inputStream));
    processRequests(myExternalizerFactory.createRequestWriter(outputStream));
  }

  protected void setPort(int port) {
    myPort = port;
  }

  private static class TimeoutInfo {
    private int myTimeout;
    private Runnable myOnTimeout;

    private TimeoutInfo(int timeout, Runnable onTimeout) {
      myTimeout = timeout;
      myOnTimeout = onTimeout;
    }
  }
}
