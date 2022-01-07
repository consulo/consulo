// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.command.impl;

import com.intellij.history.LocalHistory;
import com.intellij.history.core.LocalHistoryFacade;
import com.intellij.history.core.changes.Change;
import com.intellij.history.core.changes.ContentChange;
import com.intellij.history.core.changes.StructuralChange;
import com.intellij.history.integration.IdeaGateway;
import com.intellij.history.integration.LocalHistoryImpl;
import com.intellij.openapi.command.undo.*;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.FileContentUtilCore;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.List;

public class FileUndoProvider implements UndoProvider, BulkFileListener {
  public static final Logger LOG = Logger.getInstance(FileUndoProvider.class);

  private final Key<DocumentReference> DELETION_WAS_UNDOABLE = new Key<>(FileUndoProvider.class.getName() + ".DeletionWasUndoable");

  private final Project myProject;
  private boolean myIsInsideCommand;

  private LocalHistoryFacade myLocalHistory;
  private IdeaGateway myGateway;

  private long myLastChangeId;

  protected FileUndoProvider() {
    this(null);
  }

  protected FileUndoProvider(Project project) {
    myProject = project;
    if (myProject == null) return;

    LocalHistoryImpl localHistory = LocalHistoryImpl.getInstanceImpl();
    myLocalHistory = localHistory.getFacade();
    myGateway = localHistory.getGateway();
    if (myLocalHistory == null || myGateway == null) return; // local history was not initialized (e.g. in headless environment)

    localHistory.addVFSListenerAfterLocalHistoryOne(this, project);
    myLocalHistory.addListener(new LocalHistoryFacade.Listener() {
      @Override
      public void changeAdded(Change c) {
        if (!(c instanceof StructuralChange) || c instanceof ContentChange) return;
        myLastChangeId = c.getId();
      }
    }, myProject);
  }

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
  public void before(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent e : events) {
      if (e instanceof VFileContentChangeEvent) {
        beforeContentsChange((VFileContentChangeEvent)e);
      }
      else if (e instanceof VFileDeleteEvent) {
        beforeFileDeletion((VFileDeleteEvent)e);
      }
    }
  }

  @Override
  public void after(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent e : events) {
      if (e instanceof VFileCreateEvent || e instanceof VFileMoveEvent || e instanceof VFilePropertyChangeEvent && ((VFilePropertyChangeEvent)e).isRename()) {
        VirtualFile file = e.getFile();
        if (file != null) {
          processEvent(e, file);
        }
      }
      else if (e instanceof VFileDeleteEvent) {
        fileDeleted((VFileDeleteEvent)e);
      }
    }
  }

  private void processEvent(@Nonnull VFileEvent e, @Nonnull VirtualFile file) {
    if (!shouldProcess(e, file)) return;
    if (isUndoable(e, file)) {
      registerUndoableAction(file);
    }
    else {
      registerNonUndoableAction(file);
    }
  }

  private void beforeContentsChange(@Nonnull VFileContentChangeEvent e) {
    VirtualFile file = e.getFile();
    if (!shouldProcess(e, file)) return;
    if (isUndoable(e, file)) return;
    registerNonUndoableAction(file);
  }

  private void beforeFileDeletion(@Nonnull VFileDeleteEvent e) {
    VirtualFile file = e.getFile();
    if (!shouldProcess(e, file)) {
      invalidateActionsFor(file);
      return;
    }
    if (isUndoable(e, file)) {
      file.putUserData(DELETION_WAS_UNDOABLE, createDocumentReference(file));
    }
    else {
      registerNonUndoableAction(file);
    }
  }

  private void fileDeleted(@Nonnull VFileDeleteEvent e) {
    VirtualFile f = e.getFile();
    if (!shouldProcess(e, f)) return;

    DocumentReference ref = f.getUserData(DELETION_WAS_UNDOABLE);
    if (ref != null) {
      registerUndoableAction(ref);
      f.putUserData(DELETION_WAS_UNDOABLE, null);
    }
  }

  private boolean shouldProcess(@Nonnull VFileEvent e, VirtualFile file) {
    if (!myIsInsideCommand || myProject.isDisposed()) {
      return false;
    }

    Object requestor = e.getRequestor();
    if (FileContentUtilCore.FORCE_RELOAD_REQUESTOR.equals(requestor) /*|| requestor instanceof StorageManagerFileWriteRequestor*/) {
      return false;
    }
    return LocalHistory.getInstance().isUnderControl(file);
  }

  private static boolean isUndoable(@Nonnull VFileEvent e, @Nonnull VirtualFile file) {
    return !e.isFromRefresh() || file.getUserData(UndoConstants.FORCE_RECORD_UNDO) == Boolean.TRUE;
  }

  private void registerUndoableAction(@Nonnull VirtualFile file) {
    registerUndoableAction(createDocumentReference(file));
  }

  private void registerUndoableAction(DocumentReference ref) {
    getUndoManager().undoableActionPerformed(new MyUndoableAction(ref));
  }

  private void registerNonUndoableAction(@Nonnull VirtualFile file) {
    getUndoManager().nonundoableActionPerformed(createDocumentReference(file), true);
  }

  private void invalidateActionsFor(@Nonnull VirtualFile file) {
    if (myProject == null || !myProject.isDisposed()) {
      getUndoManager().invalidateActionsFor(createDocumentReference(file));
    }
  }

  private static DocumentReference createDocumentReference(@Nonnull VirtualFile file) {
    return DocumentReferenceManager.getInstance().create(file);
  }

  private UndoManagerImpl getUndoManager() {
    if (myProject != null) {
      return (UndoManagerImpl)UndoManager.getInstance(myProject);
    }
    return (UndoManagerImpl)UndoManager.getGlobalInstance();
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
