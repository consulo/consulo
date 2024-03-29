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
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.project.Project;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.log.LoggingHandler;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class DeploymentLogManagerImpl implements DeploymentLogManager {
  private final LoggingHandlerImpl myMainLoggingHandler;
  private final Project myProject;
  private final Map<String, LoggingHandlerImpl> myAdditionalLoggingHandlers = new HashMap<String, LoggingHandlerImpl>();
  private final Runnable myChangeListener;

  public DeploymentLogManagerImpl(@Nonnull Project project, @Nonnull Runnable changeListener) {
    myProject = project;
    myChangeListener = changeListener;
    myMainLoggingHandler = new LoggingHandlerImpl(project);
  }

  @Nonnull
  @Override
  public LoggingHandlerImpl getMainLoggingHandler() {
    return myMainLoggingHandler;
  }

  @Nonnull
  @Override
  public LoggingHandler addAdditionalLog(@Nonnull String presentableName) {
    LoggingHandlerImpl handler = new LoggingHandlerImpl(myProject);
    synchronized (myAdditionalLoggingHandlers) {
      myAdditionalLoggingHandlers.put(presentableName, handler);
    }
    myChangeListener.run();
    return handler;
  }

  @Nonnull
  public Map<String, LoggingHandlerImpl> getAdditionalLoggingHandlers() {
    HashMap<String, LoggingHandlerImpl> result;
    synchronized (myAdditionalLoggingHandlers) {
      result = new HashMap<String, LoggingHandlerImpl>(myAdditionalLoggingHandlers);
    }
    return result;
  }
}
