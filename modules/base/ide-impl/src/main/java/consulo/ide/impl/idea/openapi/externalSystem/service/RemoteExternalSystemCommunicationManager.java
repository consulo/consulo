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
package consulo.ide.impl.idea.openapi.externalSystem.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.CommonBundle;
import consulo.component.extension.ExtensionPointName;
import consulo.container.boot.ContainerPathManager;
import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.execution.runner.ProgramRunner;
import consulo.ide.impl.idea.execution.rmi.RemoteProcessSupport;
import consulo.ide.impl.idea.ide.actions.OpenProjectFileChooserDescriptor;
import consulo.ide.impl.idea.openapi.application.PathManager;
import consulo.ide.impl.idea.openapi.externalSystem.ExternalSystemManager;
import consulo.ide.impl.idea.openapi.externalSystem.model.ProjectSystemId;
import consulo.ide.impl.idea.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.ide.impl.idea.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.wrapper.ExternalSystemFacadeWrapper;
import consulo.ide.impl.idea.openapi.externalSystem.util.ExternalSystemApiUtil;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.psi.PsiBundle;
import consulo.logging.Logger;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.SimpleJavaParameters;
import consulo.process.internal.OSProcessHandler;
import consulo.project.ProjectBundle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.SystemProperties;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Denis Zhdanov
 * @since 8/9/13 3:37 PM
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class RemoteExternalSystemCommunicationManager implements ExternalSystemCommunicationManager {

  private static final Logger LOG = Logger.getInstance(RemoteExternalSystemCommunicationManager.class);

  private static final String MAIN_CLASS_NAME = RemoteExternalSystemFacadeImpl.class.getName();

  private final AtomicReference<RemoteExternalSystemProgressNotificationManager> myExportedNotificationManager
    = new AtomicReference<RemoteExternalSystemProgressNotificationManager>();

  @Nonnull
  private final ThreadLocal<ProjectSystemId> myTargetExternalSystemId = new ThreadLocal<ProjectSystemId>();

  @Nonnull
  private final ExternalSystemProgressNotificationManagerImpl                    myProgressManager;
  @Nonnull
  private final RemoteProcessSupport<Object, RemoteExternalSystemFacade, String> mySupport;

  @Inject
  public RemoteExternalSystemCommunicationManager(@Nonnull ExternalSystemProgressNotificationManager notificationManager) {
    myProgressManager = (ExternalSystemProgressNotificationManagerImpl)notificationManager;
    mySupport = new RemoteProcessSupport<Object, RemoteExternalSystemFacade, String>(RemoteExternalSystemFacade.class) {
      @Override
      protected void fireModificationCountChanged() {
      }

      @Override
      protected String getName(Object o) {
        return RemoteExternalSystemFacade.class.getName();
      }

      @Override
      protected RunProfileState getRunProfileState(Object o, String configuration, Executor executor) throws ExecutionException {
        return createRunProfileState();
      }
    };

    ShutDownTracker.getInstance().registerShutdownTask(new Runnable() {
      public void run() {
        shutdown(false);
      }
    });
  }

  public synchronized void shutdown(boolean wait) {
    mySupport.stopAll(wait);
  }

  private RunProfileState createRunProfileState() {
    return new CommandLineState(null) {
      private SimpleJavaParameters createJavaParameters() throws ExecutionException {

        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdkHome(SystemProperties.getJavaHome());

        params.setWorkingDirectory(ContainerPathManager.get().getBinPath());
        final List<String> classPath = new ArrayList<>();

        // IDE jars.
        classPath.addAll(PathManager.getUtilClassPath());
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(ProjectBundle.class));
        ExternalSystemApiUtil.addBundle(params.getClassPath(), "messages.ProjectBundle", ProjectBundle.class);
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(PsiBundle.class));
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(Alarm.class));
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(DependencyScope.class));
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(ExtensionPointName.class));
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(OpenProjectFileChooserDescriptor.class));
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(ExternalSystemTaskNotificationListener.class));

        // External system module jars
        ContainerUtil.addIfNotNull(classPath, PathUtil.getJarPathForClass(getClass()));
        ExternalSystemApiUtil.addBundle(params.getClassPath(), "messages.CommonBundle", CommonBundle.class);
        params.getClassPath().addAll(classPath);

        params.setMainClass(MAIN_CLASS_NAME);
        params.getVMParametersList().addParametersString("-Djava.awt.headless=true");

        // It may take a while for gradle api to resolve external dependencies. Default RMI timeout
        // is 15 seconds (http://download.oracle.com/javase/6/docs/technotes/guides/rmi/sunrmiproperties.html#connectionTimeout),
        // we don't want to get EOFException because of that.
        params.getVMParametersList().addParametersString(
          "-Dsun.rmi.transport.connectionTimeout=" + String.valueOf(TimeUnit.HOURS.toMillis(1))
        );
//        params.getVMParametersList().addParametersString("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5009");

        ProjectSystemId externalSystemId = myTargetExternalSystemId.get();
        if (externalSystemId != null) {
          ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
          if (manager != null) {
            params.getClassPath().add(PathUtil.getJarPathForClass(manager.getProjectResolverClass().getClass()));
            params.getProgramParametersList().add(manager.getProjectResolverClass().getName());
            params.getProgramParametersList().add(manager.getTaskManagerClass().getName());
            manager.enhanceRemoteProcessing(params);
          }
        }

        return params;
      }

      @Override
      @Nonnull
      public ExecutionResult execute(@Nonnull Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
        ProcessHandler processHandler = startProcess();
        return new DefaultExecutionResult(null, processHandler, AnAction.EMPTY_ARRAY);
      }

      @Override
      @Nonnull
      protected OSProcessHandler startProcess() throws ExecutionException {
        SimpleJavaParameters params = createJavaParameters();
        String sdk = params.getJdkHome();
        if (sdk == null) {
          throw new ExecutionException("No sdk is defined. Params: " + params);
        }
        ProcessHandler handler = params.createProcessHandler();
        ProcessTerminatedListener.attach(handler);
        return (OSProcessHandler)handler;
      }
    };
  }

  @Nullable
  @Override
  public RemoteExternalSystemFacade acquire(@Nonnull String id, @Nonnull ProjectSystemId externalSystemId)
    throws Exception
  {
    myTargetExternalSystemId.set(externalSystemId);
    final RemoteExternalSystemFacade facade;
    try {
      facade = mySupport.acquire(this, id);
    }
    finally {
      myTargetExternalSystemId.set(null);
    }
    if (facade == null) {
      return null;
    }

    RemoteExternalSystemProgressNotificationManager exported = myExportedNotificationManager.get();
    if (exported == null) {
      try {
        exported = (RemoteExternalSystemProgressNotificationManager)UnicastRemoteObject.exportObject(myProgressManager, 0);
        myExportedNotificationManager.set(exported);
      }
      catch (RemoteException e) {
        exported = myExportedNotificationManager.get();
      }
    }
    if (exported == null) {
      LOG.warn("Can't export progress manager");
    }
    else {
      facade.applyProgressManager(exported);
    }
    return facade;
  }

  @Override
  public void release(@Nonnull String id, @Nonnull ProjectSystemId externalSystemId) throws Exception {
    mySupport.release(this, id);
  }

  @Override
  public boolean isAlive(@Nonnull RemoteExternalSystemFacade facade) {
    RemoteExternalSystemFacade toCheck = facade;
    if (facade instanceof ExternalSystemFacadeWrapper) {
      toCheck = ((ExternalSystemFacadeWrapper)facade).getDelegate();

    }
    if (toCheck instanceof InProcessExternalSystemFacadeImpl) {
      return false;
    }
    try {
      facade.getResolver();
      return true;
    }
    catch (RemoteException e) {
      return false;
    }
  }

  @Override
  public void clear() {
    mySupport.stopAll(true); 
  }
}
