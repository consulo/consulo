/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.remoteServer.impl.runtime;

import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.remoteServer.runtime.ServerConnection;
import consulo.ide.impl.idea.remoteServer.runtime.ServerConnectionListener;
import consulo.component.messagebus.MessageBus;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;

/**
 * @author nik
 */
public class ServerConnectionEventDispatcher {
  private final MessageBus myMessageBus;
  private final MergingUpdateQueue myEventsQueue;

  public ServerConnectionEventDispatcher() {
    myMessageBus = ApplicationManager.getApplication().getMessageBus();
    myEventsQueue = new MergingUpdateQueue("remote server connection events", 500, false, null);
  }

  public void fireConnectionCreated(ServerConnection<?> connection) {
    myMessageBus.syncPublisher(ServerConnectionListener.class).onConnectionCreated(connection);
  }

  public void queueConnectionStatusChanged(final ServerConnectionImpl<?> connection) {
    myEventsQueue.activate();
    myEventsQueue.queue(new Update(connection) {
      @Override
      public void run() {
        myMessageBus.syncPublisher(ServerConnectionListener.class).onConnectionStatusChanged(connection);
      }
    });
  }

  public void queueDeploymentsChanged(final ServerConnectionImpl<?> connection) {
    myEventsQueue.activate();
    myEventsQueue.queue(new Update(connection) {
      @Override
      public void run() {
        myMessageBus.syncPublisher(ServerConnectionListener.class).onDeploymentsChanged(connection);
      }
    });
  }
}
