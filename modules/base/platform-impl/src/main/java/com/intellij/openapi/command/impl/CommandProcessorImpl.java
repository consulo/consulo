/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.AbnormalCommandTerminationException;
import com.intellij.openapi.command.CommandToken;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Singleton
public class CommandProcessorImpl extends CoreCommandProcessor {
  @RequiredUIAccess
  @Override
  public void finishCommand(CommandToken command, final Throwable throwable) {
    if (myCurrentCommand != command) return;
    final boolean failed;
    try {
      if (throwable instanceof AbnormalCommandTerminationException) {
        final AbnormalCommandTerminationException rollback = (AbnormalCommandTerminationException)throwable;
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          throw new RuntimeException(rollback);
        }
        failed = true;
      }
      else if (throwable != null) {
        failed = true;
        if (throwable instanceof Error) {
          throw (Error)throwable;
        }
        else if (throwable instanceof RuntimeException) throw (RuntimeException)throwable;
        CommandLog.LOG.error(throwable);
      }
      else {
        failed = false;
      }
    }
    finally {
      super.finishCommand(command, throwable);
    }
    Project project = command.getProject();
    if (failed) {
      if (project != null) {
        FileEditor editor = new FocusBasedCurrentEditorProvider().getCurrentEditor();
        final UndoManager undoManager = UndoManager.getInstance(project);
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
    return (UndoManagerImpl)(project != null ? UndoManager.getInstance(project) : UndoManager.getGlobalInstance());
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
