// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.event;

import consulo.disposer.Disposable;
import consulo.codeEditor.SelectionModel;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

/**
 * Allows to receive information about selection changes in an editor.
 *
 * @see SelectionModel#addSelectionListener(SelectionListener)
 * @see EditorEventMulticaster#addSelectionListener(SelectionListener, Disposable)
 */
public interface SelectionListener extends EventListener {
  /**
   * Called when the selected area in an editor is changed.
   *
   * @param e the event containing information about the change.
   */
  default void selectionChanged(@Nonnull SelectionEvent e) {
  }
}
