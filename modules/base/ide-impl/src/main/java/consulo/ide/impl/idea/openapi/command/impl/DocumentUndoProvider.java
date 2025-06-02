// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.event.EditorDocumentListener;
import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.undoRedo.ApplicationUndoManager;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.util.UndoConstants;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

@ExtensionImpl
public final class DocumentUndoProvider implements EditorDocumentListener {
  private static final Key<Boolean> UNDOING_EDITOR_CHANGE = Key.create("DocumentUndoProvider.UNDOING_EDITOR_CHANGE");

  @Inject
  DocumentUndoProvider() {
  }

  @Nonnull
  private static UndoManagerImpl getUndoManager(@Nullable Project project) {
    return (UndoManagerImpl)(project == null ? ApplicationUndoManager.getInstance() : ProjectUndoManager.getInstance(project));
  }

  public static void startDocumentUndo(@Nullable Document doc) {
    if (doc != null) doc.putUserData(UNDOING_EDITOR_CHANGE, Boolean.TRUE);
  }

  public static void finishDocumentUndo(@Nullable Document doc) {
    if (doc != null) doc.putUserData(UNDOING_EDITOR_CHANGE, null);
  }

  @Override
  public void beforeDocumentChange(@Nonnull DocumentEvent e) {
    Document document = e.getDocument();
    if (!shouldProcess(document)) {
      return;
    }

    handleBeforeDocumentChange(getUndoManager(null), document);

    ProjectManager projectManager = ProjectManager.getInstance();
    if (projectManager != null) {
      for (Project project : projectManager.getOpenProjects()) {
        handleBeforeDocumentChange(getUndoManager(project), document);
      }
    }
  }

  private static void handleBeforeDocumentChange(@Nonnull UndoManagerImpl undoManager, @Nonnull Document document) {
    if (undoManager.isActive() && isUndoable(undoManager, document)
        && undoManager.isUndoOrRedoInProgress()
        && !Objects.equals(document.getUserData(UNDOING_EDITOR_CHANGE), Boolean.TRUE)) {
      throw new IllegalStateException("Do not change documents during undo as it will break undo sequence.");
    }
  }

  @Override
  public void documentChanged(@Nonnull DocumentEvent e) {
    Document document = e.getDocument();
    if (!shouldProcess(document)) {
      return;
    }

    handleDocumentChanged(getUndoManager(null), document, e);
    ProjectManager projectManager = ProjectManager.getInstance();
    if (projectManager != null) {
      for (Project project : projectManager.getOpenProjects()) {
        handleDocumentChanged(getUndoManager(project), document, e);
      }
    }
  }

  private static void handleDocumentChanged(@Nonnull UndoManagerImpl undoManager, @Nonnull Document document, @Nonnull DocumentEvent e) {
    if (undoManager.isActive() && isUndoable(undoManager, document)) {
      registerUndoableAction(undoManager, e);
    }
    else {
      registerNonUndoableAction(undoManager, document);
    }
  }

  private static boolean shouldProcess(@Nonnull Document document) {
    if (!ApplicationManager.getApplication().isDispatchThread()) {
      // some light document
      return false;
    }

    return !UndoManagerImpl.isCopy(document) // if we don't ignore copy's events, we will receive notification
           // for the same event twice (from original document too)
           // and undo will work incorrectly
           && shouldRecordActions(document);
  }

  private static boolean shouldRecordActions(@Nonnull Document document) {
    if (Objects.equals(document.getUserData(UndoConstants.DONT_RECORD_UNDO), Boolean.TRUE)) return false;

    VirtualFile vFile = FileDocumentManager.getInstance().getFile(document);
    if (vFile == null) return true;
    return !Objects.equals(vFile.getUserData(AbstractFileViewProvider.FREE_THREADED), Boolean.TRUE)
        && !Objects.equals(vFile.getUserData(UndoConstants.DONT_RECORD_UNDO), Boolean.TRUE);
  }

  private static void registerUndoableAction(@Nonnull UndoManagerImpl undoManager, @Nonnull DocumentEvent e) {
    undoManager.undoableActionPerformed(new EditorChangeAction(e));
  }

  private static void registerNonUndoableAction(@Nonnull UndoManagerImpl undoManager, @Nonnull Document document) {
    DocumentReference ref = DocumentReferenceManager.getInstance().create(document);
    undoManager.nonundoableActionPerformed(ref, false);
  }

  private static boolean isUndoable(@Nonnull UndoManagerImpl undoManager, @Nonnull Document document) {
    DocumentReference ref = DocumentReferenceManager.getInstance().create(document);
    VirtualFile file = ref.getFile();

    // Allow undo even from refresh if requested
    if (file != null && Objects.equals(file.getUserData(UndoConstants.FORCE_RECORD_UNDO), Boolean.TRUE)) {
      return true;
    }
    return !UndoManagerImpl.isRefresh() || undoManager.isUndoOrRedoAvailable(ref);
  }
}
