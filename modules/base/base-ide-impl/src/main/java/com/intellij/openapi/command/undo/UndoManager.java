/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.command.undo;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import consulo.command.undo.ApplicationUndoManager;
import consulo.command.undo.ProjectUndoManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface UndoManager {
  Key<Document> ORIGINAL_DOCUMENT = Key.create("ORIGINAL_DOCUMENT");

  static UndoManager getInstance(@Nonnull Project project) {
    return project.getComponent(ProjectUndoManager.class);
  }

  static UndoManager getGlobalInstance() {
    return Application.get().getComponent(ApplicationUndoManager.class);
  }

  void undoableActionPerformed(@Nonnull UndoableAction action);

  void nonundoableActionPerformed(@Nonnull DocumentReference ref, boolean isGlobal);

  boolean isUndoInProgress();

  boolean isRedoInProgress();

  default boolean isUndoOrRedoInProgress() {
    return isUndoInProgress() || isRedoInProgress();
  }

  void undo(@Nullable FileEditor editor);

  void redo(@Nullable FileEditor editor);

  boolean isUndoAvailable(@Nullable FileEditor editor);

  boolean isRedoAvailable(@Nullable FileEditor editor);

  @Nonnull
  Pair<String, String> getUndoActionNameAndDescription(FileEditor editor);

  @Nonnull
  Pair<String, String> getRedoActionNameAndDescription(FileEditor editor);
}