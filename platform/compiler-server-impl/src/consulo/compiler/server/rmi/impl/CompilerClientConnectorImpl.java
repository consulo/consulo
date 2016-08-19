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
package consulo.compiler.server.rmi.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import consulo.compiler.server.rmi.CompilerClientConnector;
import consulo.compiler.server.rmi.CompilerClientInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.rmi.RemoteException;

/**
 * @author VISTALL
 * @since 16:08/19.08.13
 */
public class CompilerClientConnectorImpl extends CompilerClientConnector implements Disposable {
  private CompilerClientInterface myClientInterface;

  @Override
  public void setClientConnection(@Nullable CompilerClientInterface clientConnection) {
    myClientInterface = clientConnection;
  }

  @Override
  public void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) {
    if(myClientInterface != null) {
      try {
        myClientInterface.addMessage(category, message, url, lineNum, columnNum);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void dispose() {
    myClientInterface = null;
  }
}
