// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.event;

import consulo.disposer.Disposable;
import consulo.document.event.DocumentListener;
import consulo.codeEditor.EditorFactory;

/**
 * Allows to attach listeners which receive notifications about changes in any currently open
 * editor.
 *
 * @see EditorFactory#getEventMulticaster()
 */
public interface EditorEventMulticaster {
  /**
   * @deprecated Use {@link #addDocumentListener(DocumentListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addDocumentListener(DocumentListener listener);

  void addDocumentListener(DocumentListener listener, Disposable parentDisposable);

  void removeDocumentListener(DocumentListener listener);

  /**
   * @deprecated Use {@link #addEditorMouseListener(EditorMouseListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addEditorMouseListener(EditorMouseListener listener);

  void addEditorMouseListener(EditorMouseListener listener, Disposable parentDisposable);

  void removeEditorMouseListener(EditorMouseListener listener);

  /**
   * @deprecated Use {@link #addEditorMouseMotionListener(EditorMouseMotionListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addEditorMouseMotionListener(EditorMouseMotionListener listener);

  void addEditorMouseMotionListener(EditorMouseMotionListener listener, Disposable parentDisposable);

  void removeEditorMouseMotionListener(EditorMouseMotionListener listener);

  /**
   * @deprecated Use {@link #addCaretListener(CaretListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addCaretListener(CaretListener listener);

  void addCaretListener(CaretListener listener, Disposable parentDisposable);

  void removeCaretListener(CaretListener listener);

  /**
   * @deprecated Use {@link #addSelectionListener(SelectionListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addSelectionListener(SelectionListener listener);

  void addSelectionListener(SelectionListener listener, Disposable parentDisposable);

  void removeSelectionListener(SelectionListener listener);

  /**
   * @deprecated Use {@link #addVisibleAreaListener(VisibleAreaListener, Disposable)} instead to avoid leaking listeners
   */
  @Deprecated
  void addVisibleAreaListener(VisibleAreaListener listener);

  default void addVisibleAreaListener(VisibleAreaListener listener, Disposable parent) {
    throw new IllegalStateException("Not implemented");
  }

  void removeVisibleAreaListener(VisibleAreaListener listener);
}
