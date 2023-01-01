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
package consulo.codeEditor.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.EventListener;

/**
 * Allows to receive notifications about mouse movement in the editor.
 *
 * @see Editor#addEditorMouseMotionListener(EditorMouseMotionListener)
 * @see EditorEventMulticaster#addEditorMouseMotionListener(EditorMouseMotionListener)
 * @see EditorMouseListener
 */
@ExtensionAPI(ComponentScope.APPLICATION)
// FIXME [VISTALL] use @Topic not @Extension?
public interface EditorMouseMotionListener extends EventListener {
  /**
   * Called when the mouse is moved over the editor and no mouse buttons are pressed.
   *
   * @param e the event containing information about the mouse movement.
   */
  @RequiredUIAccess
  default void mouseMoved(EditorMouseEvent e) {}

  /**
   * Called when the mouse is moved over the editor and a mouse button is pressed.
   *
   * @param e the event containing information about the mouse movement.
   */
  @RequiredUIAccess
  default void mouseDragged(EditorMouseEvent e) {
  }
}
