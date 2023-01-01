// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.command.CommandToken;
import consulo.undoRedo.UndoManager;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.undoRedo.ApplicationUndoManager;
import consulo.undoRedo.ProjectUndoManager;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
@ServiceImpl
public class CommandProcessorImpl extends CoreCommandProcessor {
  @Override
  public void finishCommand(@Nonnull final CommandToken command, @Nullable final Throwable throwable) {
    if (myCurrentCommand != command) return;
    final boolean failed;
    try {
      if (throwable != null) {
        failed = true;
        ExceptionUtil.rethrowUnchecked(throwable);
        CommandLog.LOG.error(throwable);
      }
      else {
        failed = false;
      }
    }
    finally {
      try {
        super.finishCommand(command, throwable);
      }
      catch (Throwable e) {
        if (throwable != null) {
          e.addSuppressed(throwable);
        }
        throw e;
      }
    }
    if (failed) {
      Project project = command.getProject();
      if (project != null) {
        FileEditor editor = new FocusBasedCurrentEditorProvider().getCurrentEditor();
        final UndoManager undoManager = ProjectUndoManager.getInstance(project);
        if (undoManager.isUndoAvailable(editor)) {
          undoManager.undo(editor);
        }
      }
      Messages.showErrorDialog(project, "Cannot perform operation. Too complex, sorry.", "Failed to Perform Operation");
    }
  }

  @Override
  public void markCurrentCommandAsGlobal(Project project) {
    getUndoManager(project).markCurrentCommandAsGlobal();
  }

  private static UndoManagerImpl getUndoManager(Project project) {
    return (UndoManagerImpl)(project != null ? ProjectUndoManager.getInstance(project) : ApplicationUndoManager.getGlobalInstance());
  }

  @Override
  public void addAffectedDocuments(Project project, @Nonnull Document... docs) {
    getUndoManager(project).addAffectedDocuments(docs);
  }

  @Override
  public void addAffectedFiles(Project project, @Nonnull VirtualFile... files) {
    getUndoManager(project).addAffectedFiles(files);
  }
}
