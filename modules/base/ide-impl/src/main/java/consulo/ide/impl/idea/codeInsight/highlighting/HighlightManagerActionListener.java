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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.highlight.HighlightManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import jakarta.inject.Inject;

import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 21-Jun-22
 */
@TopicImpl(ComponentScope.APPLICATION)
public class HighlightManagerActionListener implements AnActionListener {
  private final ServiceLoader.Provider<HighlightManager> myHighlightManager;

  @Inject
  public HighlightManagerActionListener(ServiceLoader.Provider<HighlightManager> highlightManager) {
    myHighlightManager = highlightManager;
  }

  @Override
  public void beforeActionPerformed(AnAction action, final DataContext dataContext, AnActionEvent event) {
    requestHideHighlights(dataContext);
  }

  @Override
  public void beforeEditorTyping(char c, DataContext dataContext) {
    requestHideHighlights(dataContext);
  }

  private void requestHideHighlights(final DataContext dataContext) {
    final Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    if (editor == null) return;

    HighlightManagerImpl highlightManager = (HighlightManagerImpl)myHighlightManager.get();

    highlightManager.hideHighlights(editor, HighlightManager.HIDE_BY_ANY_KEY);
  }
}
