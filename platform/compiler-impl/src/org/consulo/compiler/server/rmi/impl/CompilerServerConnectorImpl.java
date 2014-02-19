/*
 * Copyright 2013-2014 must-be.org
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
package org.consulo.compiler.server.rmi.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import org.consulo.compiler.server.rmi.CompilerServerConnector;
import org.consulo.compiler.server.rmi.CompilerServerInterface;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 11:16/13.08.13
 */
@Logger
public class CompilerServerConnectorImpl extends CompilerServerConnector implements ApplicationComponent {
  private Future<?> myTask;
  private CompilerServerInterface myServerInterface;

  public CompilerServerConnectorImpl() {

  }

  @Override
  public void initComponent() {
    tryToConnect();
  }

  private void tryToConnect() {
    Future<?> task = myTask;
    if(task != null) {
      task.cancel(false);
    }

    myTask = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
          try {
            final Registry registry = LocateRegistry.getRegistry("localhost", 5433);

            myServerInterface = (CompilerServerInterface) registry.lookup(CompilerServerInterface.LOOKUP_ID);

            myServerInterface.notify(true);

            CompilerServerConnectorImpl.LOGGER.info("Success connected to compiler server");
          }
          catch (Exception e) {
            CompilerServerConnectorImpl.LOGGER.debug(e);
            try {
              Thread.sleep(5000L);

              tryToConnect();
            }
            catch (InterruptedException e1) {
              //
            }
        }
      }
    });
  }

  @Override
  public boolean isConnected() {
    return myServerInterface !=  null;
  }

  @Override
  public void disposeComponent() {
    Future<?> task = myTask;
    if(task != null) {
      task.cancel(false);
    }
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "CompilerSwapperManager";
  }
}
