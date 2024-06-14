/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.application;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.SaveAndSyncHandler;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.FrameStateManager;
import consulo.application.ui.event.FrameStateListener;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.document.internal.FileDocumentManagerEx;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.*;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Singleton
@ServiceImpl
public class DesktopSaveAndSyncHandlerImpl extends SaveAndSyncHandler implements Disposable {
  private static final Logger LOG = Logger.getInstance(SaveAndSyncHandler.class);

  private final Application myApplication;
  private final Runnable myIdleListener;
  private final PropertyChangeListener myGeneralSettingsListener;
  private final GeneralSettings mySettings;
  private final ProgressManager myProgressManager;
  private final AtomicInteger myBlockSaveOnFrameDeactivationCount = new AtomicInteger();
  private final AtomicInteger myBlockSyncOnFrameActivationCount = new AtomicInteger();
  private volatile long myRefreshSessionId;

  private Future<?> myRefreshDelayAlarm = CompletableFuture.completedFuture(null);

  @Inject
  public DesktopSaveAndSyncHandlerImpl(@Nonnull Application application,
                                       @Nonnull GeneralSettings generalSettings,
                                       @Nonnull ProgressManager progressManager,
                                       @Nonnull FrameStateManager frameStateManager,
                                       @Nonnull FileDocumentManager fileDocumentManager) {
    mySettings = generalSettings;
    myApplication = application;
    myProgressManager = progressManager;

    myIdleListener = () -> {
      if (mySettings.isAutoSaveIfInactive() && canSyncOrSave()) {
        ((FileDocumentManagerEx)fileDocumentManager).saveAllDocuments(false);
      }
    };
    IdeEventQueue.getInstance().addIdleListener(myIdleListener, mySettings.getInactiveTimeout() * 1000);

    myGeneralSettingsListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(@Nonnull PropertyChangeEvent e) {
        if (GeneralSettings.PROP_INACTIVE_TIMEOUT.equals(e.getPropertyName())) {
          IdeEventQueue eventQueue = IdeEventQueue.getInstance();
          eventQueue.removeIdleListener(myIdleListener);
          Integer timeout = (Integer)e.getNewValue();
          eventQueue.addIdleListener(myIdleListener, timeout.intValue() * 1000);
        }
      }
    };
    mySettings.addPropertyChangeListener(myGeneralSettingsListener);

    frameStateManager.addListener(new FrameStateListener() {
      @Override
      public void onFrameDeactivated() {
        LOG.debug("save(): enter");
        if (canSyncOrSave()) {
          saveProjectsAndDocuments();
        }
        LOG.debug("save(): exit");
      }

      @Override
      public void onFrameActivated() {
        if (!myApplication.isDisposed() && mySettings.isSyncOnFrameActivation()) {
          scheduleRefresh();
        }
      }
    });
  }

  @Override
  public void dispose() {
    RefreshQueue.getInstance().cancelSession(myRefreshSessionId);
    mySettings.removePropertyChangeListener(myGeneralSettingsListener);
    IdeEventQueue.getInstance().removeIdleListener(myIdleListener);
  }

  private boolean canSyncOrSave() {
    return !LaterInvocator.isInModalContext() && !myProgressManager.hasModalProgressIndicator();
  }

  @Override
  public void saveProjectsAndDocuments() {
    if (!myApplication.isDisposed() && mySettings.isSaveOnFrameDeactivation() && myBlockSaveOnFrameDeactivationCount.get() == 0) {
      myApplication.saveAll();
    }
  }

  @Override
  public void scheduleRefresh() {
    myRefreshDelayAlarm.cancel(false);
    myRefreshDelayAlarm = myApplication.getLastUIAccess().getScheduler().schedule(this::doScheduledRefresh, 300, TimeUnit.MILLISECONDS);
  }

  private void doScheduledRefresh() {
    if (canSyncOrSave()) {
      refreshOpenFiles();
    }
    maybeRefresh(myApplication.getNoneModalityState());
  }

  public void maybeRefresh(@Nonnull ModalityState modalityState) {
    if (myBlockSyncOnFrameActivationCount.get() == 0 && mySettings.isSyncOnFrameActivation()) {
      RefreshQueue queue = RefreshQueue.getInstance();
      queue.cancelSession(myRefreshSessionId);

      RefreshSession session = queue.createSession(true, true, null, modalityState);
      session.addAllFiles(ManagingFS.getInstance().getLocalRoots());
      myRefreshSessionId = session.getId();
      session.launch();
      LOG.debug("vfs refreshed");
    }
    else if (LOG.isDebugEnabled()) {
      LOG.debug("vfs refresh rejected, blocked: " + (myBlockSyncOnFrameActivationCount.get() != 0) + ", isSyncOnFrameActivation: " + mySettings
        .isSyncOnFrameActivation());
    }
  }

  @Override
  public void refreshOpenFiles() {
    List<VirtualFile> files = new ArrayList<>();

    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      for (VirtualFile file : FileEditorManager.getInstance(project).getSelectedFiles()) {
        if (file instanceof VirtualFileWithId) {
          files.add(file);
        }
      }
    }

    if (!files.isEmpty()) {
      // refresh open files synchronously so it doesn't wait for potentially longish refresh request in the queue to finish
      RefreshQueue.getInstance().refresh(false, false, null, files);
    }
  }

  @Override
  public void blockSaveOnFrameDeactivation() {
    LOG.debug("save blocked");
    myBlockSaveOnFrameDeactivationCount.incrementAndGet();
  }

  @Override
  public void unblockSaveOnFrameDeactivation() {
    myBlockSaveOnFrameDeactivationCount.decrementAndGet();
    LOG.debug("save unblocked");
  }

  @Override
  public void blockSyncOnFrameActivation() {
    LOG.debug("sync blocked");
    myBlockSyncOnFrameActivationCount.incrementAndGet();
  }

  @Override
  public void unblockSyncOnFrameActivation() {
    myBlockSyncOnFrameActivationCount.decrementAndGet();
    LOG.debug("sync unblocked");
  }
}