// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.event;

import consulo.disposer.Disposable;
import javax.annotation.Nonnull;

/**
 * Allows to attach listeners which receive notifications about changes in any currently open
 * editor.
 *
 * @see com.intellij.openapi.editor.EditorFactory#getEventMulticaster()
 */
public interface EditorEventMulticaster {
  /**
   * @deprecated Use {@link #addDocumentListener(DocumentListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addDocumentListener(@Nonnull DocumentListener listener);

  void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable);

  void removeDocumentListener(@Nonnull DocumentListener listener);

  /**
   * @deprecated Use {@link #addEditorMouseListener(EditorMouseListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addEditorMouseListener(@Nonnull EditorMouseListener listener);

  void addEditorMouseListener(@Nonnull EditorMouseListener listener, @Nonnull Disposable parentDisposable);

  void removeEditorMouseListener(@Nonnull EditorMouseListener listener);

  /**
   * @deprecated Use {@link #addEditorMouseMotionListener(EditorMouseMotionListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener);

  void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener, @Nonnull Disposable parentDisposable);

  void removeEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener);

  /**
   * @deprecated Use {@link #addCaretListener(CaretListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addCaretListener(@Nonnull CaretListener listener);

  void addCaretListener(@Nonnull CaretListener listener, @Nonnull Disposable parentDisposable);

  void removeCaretListener(@Nonnull CaretListener listener);

  /**
   * @deprecated Use {@link #addSelectionListener(SelectionListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addSelectionListener(@Nonnull SelectionListener listener);

  void addSelectionListener(@Nonnull SelectionListener listener, @Nonnull Disposable parentDisposable);

  void removeSelectionListener(@Nonnull SelectionListener listener);

  /**
   * @deprecated Use {@link #addVisibleAreaListener(VisibleAreaListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addVisibleAreaListener(@Nonnull VisibleAreaListener listener);

  default void addVisibleAreaListener(@Nonnull VisibleAreaListener listener, @Nonnull Disposable parent) {
    throw new IllegalStateException("Not implemented");
  }

  void removeVisibleAreaListener(@Nonnull VisibleAreaListener listener);
}
