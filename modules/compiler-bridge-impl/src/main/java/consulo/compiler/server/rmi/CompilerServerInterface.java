/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author VISTALL
 * @since 11:12/13.08.13
 */
public interface CompilerServerInterface extends Remote {
  String LOOKUP_ID = "//localhost/ConsuloCompilerServer";

  void notify(boolean connected) throws RemoteException;

  void compile(final CompilerClientInterface client) throws RemoteException;

  void shutdown() throws RemoteException;
}
