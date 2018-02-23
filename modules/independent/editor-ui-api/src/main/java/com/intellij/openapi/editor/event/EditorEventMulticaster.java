/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.editor.event;

import com.intellij.openapi.Disposable;
import javax.annotation.Nonnull;

/**
 * Allows to attach listeners which receive notifications about changes in any currently open
 * editor.
 *
 * @see com.intellij.openapi.editor.EditorFactory#getEventMulticaster() 
 */
public interface EditorEventMulticaster {
  void addDocumentListener(@Nonnull DocumentListener listener);
  void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable);
  void removeDocumentListener(@Nonnull DocumentListener listener);

  void addEditorMouseListener(@Nonnull EditorMouseListener listener);
  void addEditorMouseListener(@Nonnull EditorMouseListener listener, @Nonnull Disposable parentDisposable);
  void removeEditorMouseListener(@Nonnull EditorMouseListener listener);

  void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener);
  void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener, @Nonnull Disposable parentDisposable);
  void removeEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener);

  void addCaretListener(@Nonnull CaretListener listener);
  void addCaretListener(@Nonnull CaretListener listener, @Nonnull Disposable parentDisposable);
  void removeCaretListener(@Nonnull CaretListener listener);

  void addSelectionListener(@Nonnull SelectionListener listener);
  void addSelectionListener(@Nonnull SelectionListener listener, @Nonnull Disposable parentDisposable);
  void removeSelectionListener(@Nonnull SelectionListener listener);

  void addVisibleAreaListener(@Nonnull VisibleAreaListener listener);
  void removeVisibleAreaListener(@Nonnull VisibleAreaListener listener);
}
