/*
 * Copyright 2013 Consulo.org
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
package org.consulo.compiler.server.rmi;

import com.intellij.openapi.compiler.CompilerMessageCategory;
import org.jetbrains.annotations.NotNull;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author VISTALL
 * @since 11:14/13.08.13
 */
public interface CompilerClientInterface extends Remote {
  void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) throws RemoteException;

  void compilationFinished(boolean aborted, int errors, int warnings) throws RemoteException;

  @NotNull
  String getProjectDir() throws RemoteException;
}
