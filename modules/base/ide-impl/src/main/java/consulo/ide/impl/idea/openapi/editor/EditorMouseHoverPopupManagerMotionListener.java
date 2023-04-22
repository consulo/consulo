/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.openapi.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.language.editor.impl.internal.hint.EditorMouseHoverPopupManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

@ExtensionImpl
final class EditorMouseHoverPopupManagerMotionListener implements EditorMouseMotionListener {
  private final Provider<EditorMouseHoverPopupManager> myEditorMouseHoverPopupManager;

  @Inject
  EditorMouseHoverPopupManagerMotionListener(Provider<EditorMouseHoverPopupManager> editorMouseHoverPopupManager) {
    myEditorMouseHoverPopupManager = editorMouseHoverPopupManager;
  }

  @RequiredUIAccess
  @Override
  public void mouseMoved(@Nonnull EditorMouseEvent e) {
    myEditorMouseHoverPopupManager.get().handleMouseMoved(e);
  }
}
