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
package org.consulo.compiler.server.rmi.impl;

import org.consulo.compiler.server.rmi.CompilerSwapper;
import org.consulo.compiler.server.rmi.CompilerSwapperClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author VISTALL
 * @since 11:14/13.08.13
 */
public class RemoteCompilerSwapperImpl extends UnicastRemoteObject implements CompilerSwapper {
  public RemoteCompilerSwapperImpl() throws RemoteException {
  }

  @Override
  public void compileProject(CompilerSwapperClient client, String projectDir) throws RemoteException {
    System.out.println("Compile: " + projectDir);

    client.addMessage("Test");
  }
}
