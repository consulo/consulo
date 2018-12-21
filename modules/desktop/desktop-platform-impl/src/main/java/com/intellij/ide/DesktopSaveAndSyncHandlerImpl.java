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
package com.intellij.ide;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import com.intellij.util.SingleAlarm;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotations.RequiredWriteAction;
import consulo.application.AccessRule;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Singleton
public class DesktopSaveAndSyncHandlerImpl extends SaveAndSyncHandler implements Disposable {
  private static final Logger LOG = Logger.getInstance(SaveAndSyncHandler.class);

  private final Runnable myIdleListener;
  private final PropertyChangeListener myGeneralSettingsListener;
  @Nonnull
  private final Application myApplication;
  private final GeneralSettings mySettings;
  private final ProgressManager myProgressManager;
  private final SingleAlarm myRefreshDelayAlarm = new SingleAlarm(this::doScheduledRefresh, 300, this);
  private final AtomicInteger myBlockSaveOnFrameDeactivationCount = new AtomicInteger();
  private final AtomicInteger myBlockSyncOnFrameActivationCount = new AtomicInteger();
  private volatile long myRefreshSessionId;

  @Inject
  public DesktopSaveAndSyncHandlerImpl(@Nonnull Application application,
                                       @Nonnull GeneralSettings generalSettings,
                                       @Nonnull ProgressManager progressManager,
                                       @Nonnull FrameStateManager frameStateManager,
                                       @Nonnull FileDocumentManager fileDocumentManager) {
    myApplication = application;
    mySettings = generalSettings;
    myProgressManager = progressManager;

    myIdleListener = () -> {
      UIAccess uiAccess = UIAccess.current();
      if (mySettings.isAutoSaveIfInactive() && canSyncOrSave(uiAccess)) {
        TransactionGuard.submitTransaction(myApplication, () -> ((FileDocumentManagerImpl)fileDocumentManager).saveAllDocuments(false));
      }
    };
    IdeEventQueue.getInstance().addIdleListener(myIdleListener, mySettings.getInactiveTimeout() * 1000);

    myGeneralSettingsListener = e -> {
      if (GeneralSettings.PROP_INACTIVE_TIMEOUT.equals(e.getPropertyName())) {
        IdeEventQueue eventQueue = IdeEventQueue.getInstance();
        eventQueue.removeIdleListener(myIdleListener);
        Integer timeout = (Integer)e.getNewValue();
        eventQueue.addIdleListener(myIdleListener, timeout * 1000);
      }
    };

    mySettings.addPropertyChangeListener(myGeneralSettingsListener);

    frameStateManager.addListener(new FrameStateListener() {
      @RequiredUIAccess
      @Override
      public void onFrameDeactivated() {
        UIAccess uiAccess = UIAccess.current();

        LOG.debug("save(): enter");
        AccessRule.writeAsync(() -> {
          if (canSyncOrSave(uiAccess)) {
            saveProjectsAndDocuments();
          }
          LOG.debug("save(): exit");
        });
      }

      @RequiredUIAccess
      @Override
      public void onFrameActivated() {
        if (!application.isDisposed() && mySettings.isSyncOnFrameActivation()) {
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

  @Override
  @RequiredWriteAction
  public void saveProjectsAndDocuments() {
    if (!myApplication.isDisposed() && mySettings.isSaveOnFrameDeactivation() && myBlockSaveOnFrameDeactivationCount.get() == 0) {
      myApplication.saveAll();
    }
  }

  @Override
  public void scheduleRefresh() {
    myRefreshDelayAlarm.cancelAndRequest();
  }

  private boolean canSyncOrSave(UIAccess uiAccess) {
    return !LaterInvocator.isInModalContext(uiAccess) && !myProgressManager.hasModalProgressIndicator();
  }

  private void doScheduledRefresh() {
    UIAccess lastUIAccess = myApplication.getLastUIAccess();
    if (canSyncOrSave(lastUIAccess)) {
      refreshOpenFiles();
    }
    maybeRefresh(ModalityState.NON_MODAL);
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
      LOG.debug("vfs refresh rejected, blocked: " + (myBlockSyncOnFrameActivationCount.get() != 0) + ", isSyncOnFrameActivation: " + mySettings.isSyncOnFrameActivation());
    }
  }

  @Override
  public void refreshOpenFiles() {
    List<VirtualFile> files = ContainerUtil.newArrayList();

    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      for (VirtualFile file : FileEditorManager.getInstance(project).getSelectedFiles()) {
        if (file instanceof NewVirtualFile) {
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