// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.codeEditor.Editor;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

/**
 * Allows to receive information about mouse clicks in an editor.
 *
 * @see Editor#addEditorMouseListener(EditorMouseListener)
 * @see EditorEventMulticaster#addEditorMouseListener(EditorMouseListener, Disposable)
 * @see EditorMouseMotionListener
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface EditorMouseListener extends EventListener {

  /**
   * Called when a mouse button is pressed over the editor.
   * <p/>
   * <b>Note:</b> this callback is assumed to be at the very start of 'mouse press' processing, i.e. common actions
   * like 'caret position change', 'selection change' etc implied by the 'mouse press' have not been performed yet.
   *
   * @param event the event containing information about the mouse press.
   */
  default void mousePressed(@Nonnull EditorMouseEvent event) {
  }

  /**
   * Called when a mouse button is clicked over the editor.
   *
   * @param event the event containing information about the mouse click.
   */
  default void mouseClicked(@Nonnull EditorMouseEvent event) {
  }

  /**
   * Called when a mouse button is released over the editor.
   *
   * @param event the event containing information about the mouse release.
   */
  default void mouseReleased(@Nonnull EditorMouseEvent event) {
  }

  /**
   * Called when the mouse enters the editor.
   *
   * @param event the event containing information about the mouse movement.
   */
  default void mouseEntered(@Nonnull EditorMouseEvent event) {
  }

  /**
   * Called when the mouse exits the editor.
   *
   * @param event the event containing information about the mouse movement.
   */
  default void mouseExited(@Nonnull EditorMouseEvent event) {
  }
}
