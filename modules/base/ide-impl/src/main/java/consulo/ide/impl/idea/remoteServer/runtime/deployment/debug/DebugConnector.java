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
package consulo.ide.impl.idea.remoteServer.runtime.deployment.debug;

import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentRuntime;
import javax.annotation.Nonnull;

/**
 * Implement this class if a server supports deployment in debug mode. When an user starts a deployment run configuration using 'Debug' button
 * the following happens:
 * <ul>
 *  <li>deployment process is started as usual by calling {@link consulo.ide.impl.idea.remoteServer.runtime.deployment.ServerRuntimeInstance#deploy deploy}
 *  method; you can check whether deployment is started in debug mode or not by using {@link consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentTask#isDebugMode() task.isDebugMode()} method</li>
 *  <li>when deployment is finished successfully {@link #getConnectionData} method
 *  is called to retrieve information necessary for debugger from the deployed instance</li>
 *  <li>{@link DebuggerLauncher} is used to start debugging</li>
 * </ul>
 *
 * @author nik
 * @see consulo.ide.impl.idea.remoteServer.ServerType#createDebugConnector()
 * @see consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentTask#isDebugMode()
 */
public abstract class DebugConnector<D extends DebugConnectionData, R extends DeploymentRuntime> {
  /**
   * @see consulo.ide.impl.idea.remoteServer.runtime.deployment.debug.JavaDebuggerLauncher#getInstance()
   */
  @Nonnull
  public abstract DebuggerLauncher<D> getLauncher();

  @Nonnull
  public abstract D getConnectionData(@Nonnull R runtime) throws DebugConnectionDataNotAvailableException;
}
