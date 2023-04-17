/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.compiler.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.compiler.CompilerBundle;
import consulo.compiler.TranslatingCompilerFilesMonitorHelper;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ide.impl.compiler.TranslatingCompilerFilesMonitor;
import consulo.ide.impl.compiler.TranslationCompilerProjectMonitor;
import consulo.logging.Logger;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.extension.event.ModuleExtensionChangeListener;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
* @author VISTALL
* @since 18-Jun-22
*/
@TopicImpl(ComponentScope.APPLICATION)
class TranslationCompilerProjectListener implements ProjectManagerListener {
  private static final Logger LOG = Logger.getInstance(TranslationCompilerProjectListener.class);

  private final Provider<TranslatingCompilerFilesMonitor> myMonitorProvider;

  @Inject
  TranslationCompilerProjectListener(Provider<TranslatingCompilerFilesMonitor> monitorProvider) {
    myMonitorProvider = monitorProvider;
  }

  @Override
  public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

    final MessageBusConnection conn = project.getMessageBus().connect();
    final TranslatingCompilerFilesMonitorImpl.ProjectRef projRef = new TranslatingCompilerFilesMonitorImpl.ProjectRef(project);
    final int projectId = monitor.getProjectId(project);

    monitor.watchProject(project);

    conn.subscribe(ModuleExtensionChangeListener.class, (oldExtension, newExtension) -> {
      for (TranslatingCompilerFilesMonitorHelper helper : TranslatingCompilerFilesMonitorHelper.EP_NAME.getExtensionList()) {
        if (helper.isModuleExtensionAffectToCompilation(newExtension)) {
          monitor.myForceCompiling = true;
          break;
        }
      }
    });

    conn.subscribe(ModuleRootListener.class, new ModuleRootListener() {
      private VirtualFile[] myRootsBefore;
      private Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);

      @Override
      public void beforeRootsChange(final ModuleRootEvent event) {
        if (monitor.isSuspended(projectId)) {
          return;
        }
        try {
          myRootsBefore = monitor.getRootsForScan(projRef.get());
        }
        catch (TranslatingCompilerFilesMonitorImpl.ProjectRef.ProjectClosedException e) {
          myRootsBefore = null;
        }
      }

      @Override
      public void rootsChanged(final ModuleRootEvent event) {
        if (monitor.isSuspended(projectId)) {
          return;
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Before roots changed for projectId=" + projectId + "; url=" + project.getPresentableUrl());
        }
        try {
          final VirtualFile[] rootsBefore = myRootsBefore;
          myRootsBefore = null;
          final VirtualFile[] rootsAfter = monitor.getRootsForScan(projRef.get());
          final Set<VirtualFile> newRoots = new HashSet<>();
          final Set<VirtualFile> oldRoots = new HashSet<>();
          {
            if (rootsAfter.length > 0) {
              ContainerUtil.addAll(newRoots, rootsAfter);
            }
            if (rootsBefore != null) {
              newRoots.removeAll(Arrays.asList(rootsBefore));
            }
          }
          {
            if (rootsBefore != null) {
              ContainerUtil.addAll(oldRoots, rootsBefore);
            }
            if (!oldRoots.isEmpty() && rootsAfter.length > 0) {
              oldRoots.removeAll(Arrays.asList(rootsAfter));
            }
          }

          myAlarm.cancelAllRequests(); // need alarm to deal with multiple rootsChanged events
          myAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
              monitor.startAsyncScan(projectId);
              new Task.Backgroundable(project, CompilerBundle.message("compiler.initial.scanning.progress.text"), false) {
                @Override
                public void run(@Nonnull final ProgressIndicator indicator) {
                  try {
                    if (newRoots.size() > 0) {
                      monitor.scanSourceContent(projRef, newRoots, newRoots.size(), true);
                    }
                    if (oldRoots.size() > 0) {
                      monitor.scanSourceContent(projRef, oldRoots, oldRoots.size(), false);
                    }
                    monitor.markOldOutputRoots(projRef, TranslationCompilerProjectMonitor.getInstance(projRef.get()).buildOutputRootsLayout());
                  }
                  catch (TranslatingCompilerFilesMonitorImpl.ProjectRef.ProjectClosedException swallowed) {
                    // ignored
                  }
                  finally {
                    monitor.terminateAsyncScan(projectId, false);
                  }
                }
              }.queue();
            }
          }, 500, IdeaModalityState.NON_MODAL);
        }
        catch (TranslatingCompilerFilesMonitorImpl.ProjectRef.ProjectClosedException e) {
          LOG.info(e);
        }
      }
    });
  }

  @Override
  public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

    final int projectId = monitor.getProjectId(project);
    monitor.terminateAsyncScan(projectId, true);
    synchronized (monitor.myDataLock) {
      monitor.mySourcesToRecompile.remove(projectId);
      monitor.myOutputsToDelete.remove(projectId);  // drop cache to save memory
    }
  }

  @Nonnull
  TranslatingCompilerFilesMonitorImpl getMonitor() {
    return (TranslatingCompilerFilesMonitorImpl)myMonitorProvider.get();
  }
}
