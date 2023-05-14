/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.impl.internal.hint.EditorMouseHoverPopupManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22/04/2023
 */
@TopicImpl(ComponentScope.APPLICATION)
public class EditorPopupAnActionListener implements AnActionListener {
  private final Provider<EditorMouseHoverPopupManager> myEditorMouseHoverPopupManager;

  @Inject
  public EditorPopupAnActionListener(Provider<EditorMouseHoverPopupManager> editorMouseHoverPopupManager) {
    myEditorMouseHoverPopupManager = editorMouseHoverPopupManager;
  }

  @Override
  public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
    if (action instanceof HintManagerImpl.ActionToIgnore) return;
    myEditorMouseHoverPopupManager.get().cancelProcessingAndCloseHint();
  }

  @Override
  public void beforeEditorTyping(char c, @Nonnull DataContext dataContext) {
    myEditorMouseHoverPopupManager.get().cancelProcessingAndCloseHint();
  }
}
