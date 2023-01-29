/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.change;

import consulo.application.ApplicationManager;
import consulo.application.SaveAndSyncHandler;
import consulo.document.FileDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectManagerEx;
import consulo.versionControlSystem.internal.ChangeListManagerEx;

import javax.annotation.Nonnull;

/**
 * Executes an action surrounding it with freezing-unfreezing of the ChangeListManager
 * and blocking/unblocking save/sync on frame de/activation.
 */
public class VcsFreezingProcess {

  private static final Logger LOG = Logger.getInstance(VcsFreezingProcess.class);

  @Nonnull
  private final String myReason;
  @Nonnull
  private final Runnable myRunnable;

  @Nonnull
  private final ChangeListManagerEx myChangeListManager;
  @Nonnull
  private final ProjectManagerEx myProjectManager;
  @Nonnull
  private final SaveAndSyncHandler mySaveAndSyncHandler;

  public VcsFreezingProcess(@Nonnull Project project, @Nonnull String operationTitle, @Nonnull Runnable runnable) {
    myReason = operationTitle;
    myRunnable = runnable;

    myChangeListManager = (ChangeListManagerEx)ChangeListManager.getInstance(project);
    myProjectManager = (ProjectManagerEx)ProjectManager.getInstance();
    mySaveAndSyncHandler = SaveAndSyncHandler.getInstance();
  }

  public void execute() {
    LOG.debug("starting");
    try {
      LOG.debug("saving documents, blocking project autosync");
      saveAndBlockInAwt();
      LOG.debug("freezing the ChangeListManager");
      freeze();
      try {
        LOG.debug("running the operation");
        myRunnable.run();
        LOG.debug("operation completed.");
      }
      finally {
        LOG.debug("unfreezing the ChangeListManager");
        unfreeze();
      }
    }
    finally {
      LOG.debug("unblocking project autosync");
      unblockInAwt();
    }
    LOG.debug("finished.");
  }

  private void saveAndBlockInAwt() {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      myProjectManager.blockReloadingProjectOnExternalChanges();
      FileDocumentManager.getInstance().saveAllDocuments();
      mySaveAndSyncHandler.blockSaveOnFrameDeactivation();
      mySaveAndSyncHandler.blockSyncOnFrameActivation();
    });
  }

  private void unblockInAwt() {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      myProjectManager.unblockReloadingProjectOnExternalChanges();
      mySaveAndSyncHandler.unblockSaveOnFrameDeactivation();
      mySaveAndSyncHandler.unblockSyncOnFrameActivation();
    });
  }

  private void freeze() {
    myChangeListManager.freeze(myReason);
  }

  private void unfreeze() {
    myChangeListManager.letGo();
  }
}
