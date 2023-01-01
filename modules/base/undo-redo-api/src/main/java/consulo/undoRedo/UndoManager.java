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
package consulo.undoRedo;

import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @see ProjectUndoManager
 * @see ApplicationUndoManager
 */
public interface UndoManager {
  Key<Document> ORIGINAL_DOCUMENT = Key.create("ORIGINAL_DOCUMENT");

  void undoableActionPerformed(@Nonnull UndoableAction action);

  void nonundoableActionPerformed(@Nonnull DocumentReference ref, boolean isGlobal);

  boolean isUndoInProgress();

  boolean isRedoInProgress();

  default boolean isUndoOrRedoInProgress() {
    return isUndoInProgress() || isRedoInProgress();
  }

  /**
   * @param editor instanceof FileEditor
   */
  void undo(@Nullable Object editor);

  /**
   * @param editor instanceof FileEditor
   */
  void redo(@Nullable Object editor);

  /**
   * @param editor instanceof FileEditor
   */
  boolean isUndoAvailable(@Nullable Object editor);

  /**
   * @param editor instanceof FileEditor
   */
  boolean isRedoAvailable(@Nullable Object editor);

  /**
   * @param editor instanceof FileEditor
   */
  @Nonnull
  Pair<String, String> getUndoActionNameAndDescription(Object editor);

  /**
   * @param editor instanceof FileEditor
   */
  @Nonnull
  Pair<String, String> getRedoActionNameAndDescription(Object editor);
}