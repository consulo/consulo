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

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import consulo.compiler.server.rmi.CompilerClientConnector;
import consulo.compiler.server.rmi.CompilerClientInterface;
import consulo.compiler.server.rmi.CompilerServerInterface;
import consulo.lombok.annotations.Logger;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author VISTALL
 * @since 11:14/13.08.13
 */
@Logger
public class CompilerServerInterfaceImpl extends UnicastRemoteObject implements CompilerServerInterface {
  public CompilerServerInterfaceImpl() throws RemoteException {
  }

  @Override
  public void notify(boolean connected) throws RemoteException {
    LOGGER.info("Client connected");
  }

  @Override
  public void compile(final CompilerClientInterface client) throws RemoteException {
    try {
      final Project project = ProjectManagerEx.getInstanceEx().loadProject(client.getProjectDir());

      assert project != null;

      ((ProjectManagerImpl)ProjectManagerEx.getInstance()).fireProjectOpened(project);

      CompilerClientConnector.getInstance(project).setClientConnection(client);

      CompilerManager.getInstance(project).rebuild(new CompileStatusNotification() {
        @Override
        public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
          try {
            client.compilationFinished(aborted, errors, warnings);
          }
          catch (RemoteException e) {
            //
          }

          project.dispose();
        }
      });
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  @Override
  public void shutdown() throws RemoteException {
    System.exit(0);
  }
}
