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

package com.intellij.openapi.command.impl;

import com.intellij.history.LocalHistory;
import com.intellij.history.core.LocalHistoryFacade;
import com.intellij.history.integration.IdeaGateway;
import com.intellij.openapi.command.undo.*;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.*;
import com.intellij.util.FileContentUtilCore;

import javax.annotation.Nonnull;
import java.io.IOException;

public abstract class FileUndoProvider implements UndoProvider, VirtualFileListener {
  public static final Logger LOG = Logger.getInstance(FileUndoProvider.class);

  private final Key<DocumentReference> DELETION_WAS_UNDOABLE = Key.create(FileUndoProvider.class.getName() + ".DeletionWasUndoable");

  protected Project myProject;
  protected boolean myIsInsideCommand;

  protected LocalHistoryFacade myLocalHistory;
  protected IdeaGateway myGateway;

  protected long myLastChangeId;

  @Override
  public void commandStarted(Project p) {
    if (myProject != p) return;
    myIsInsideCommand = true;
  }

  @Override
  public void commandFinished(Project p) {
    if (myProject != p) return;
    myIsInsideCommand = false;
  }

  @Override
  public void fileCreated(@Nonnull VirtualFileEvent e) {
    processEvent(e);
  }

  @Override
  public void propertyChanged(@Nonnull VirtualFilePropertyEvent e) {
    if (!e.getPropertyName().equals(VirtualFile.PROP_NAME)) return;
    processEvent(e);
  }

  @Override
  public void fileMoved(@Nonnull VirtualFileMoveEvent e) {
    processEvent(e);
  }

  private void processEvent(VirtualFileEvent e) {
    if (!shouldProcess(e)) return;
    if (isUndoable(e)) {
      registerUndoableAction(e);
    }
    else {
      registerNonUndoableAction(e);
    }
  }

  @Override
  public void beforeContentsChange(@Nonnull VirtualFileEvent e) {
    if (!shouldProcess(e)) return;
    if (isUndoable(e)) return;
    registerNonUndoableAction(e);
  }

  @Override
  public void beforeFileDeletion(@Nonnull VirtualFileEvent e) {
    if (!shouldProcess(e)) {
      invalidateActionsFor(e);
      return;
    }
    if (isUndoable(e)) {
      VirtualFile file = e.getFile();
      file.putUserData(DELETION_WAS_UNDOABLE, createDocumentReference(e));
    }
    else {
      registerNonUndoableAction(e);
    }
  }

  @Override
  public void fileDeleted(@Nonnull VirtualFileEvent e) {
    if (!shouldProcess(e)) return;
    VirtualFile f = e.getFile();

    DocumentReference ref = f.getUserData(DELETION_WAS_UNDOABLE);
    if (ref != null) {
      registerUndoableAction(ref);
      f.putUserData(DELETION_WAS_UNDOABLE, null);
    }
  }

  private boolean shouldProcess(VirtualFileEvent e) {
    return !myProject.isDisposed() &&
           LocalHistory.getInstance().isUnderControl(e.getFile()) &&
           myIsInsideCommand &&
           !FileContentUtilCore.FORCE_RELOAD_REQUESTOR.equals(e.getRequestor());
  }

  private static boolean isUndoable(VirtualFileEvent e) {
    return !e.isFromRefresh() || e.getFile().getUserData(UndoConstants.FORCE_RECORD_UNDO) == Boolean.TRUE;
  }

  private void registerUndoableAction(VirtualFileEvent e) {
    registerUndoableAction(createDocumentReference(e));
  }

  private void registerUndoableAction(DocumentReference ref) {
    getUndoManager().undoableActionPerformed(new MyUndoableAction(ref));
  }

  private void registerNonUndoableAction(VirtualFileEvent e) {
    getUndoManager().nonundoableActionPerformed(createDocumentReference(e), true);
  }

  private void invalidateActionsFor(VirtualFileEvent e) {
    if (myProject == null || !myProject.isDisposed()) {
      ((UndoManagerImpl)getUndoManager()).invalidateActionsFor(createDocumentReference(e));
    }
  }

  private static DocumentReference createDocumentReference(VirtualFileEvent e) {
    return DocumentReferenceManager.getInstance().create(e.getFile());
  }

  private UndoManager getUndoManager() {
    if (myProject != null) {
      return UndoManager.getInstance(myProject);
    }
    return UndoManager.getGlobalInstance();
  }

  private class MyUndoableAction extends GlobalUndoableAction {
    private ChangeRange myActionChangeRange;
    private ChangeRange myUndoChangeRange;

    MyUndoableAction(DocumentReference r) {
      super(r);
      myActionChangeRange = new ChangeRange(myGateway, myLocalHistory, myLastChangeId);
    }

    @Override
    public void undo() throws UnexpectedUndoException {
      try {
        myUndoChangeRange = myActionChangeRange.revert(myUndoChangeRange);
      }
      catch (IOException e) {
        LOG.warn(e);
        throw new UnexpectedUndoException(e.getMessage());
      }
    }

    @Override
    public void redo() throws UnexpectedUndoException {
      try {
        myActionChangeRange = myUndoChangeRange.revert(myActionChangeRange);
      }
      catch (IOException e) {
        LOG.warn(e);
        throw new UnexpectedUndoException(e.getMessage());
      }
    }
  }
}
