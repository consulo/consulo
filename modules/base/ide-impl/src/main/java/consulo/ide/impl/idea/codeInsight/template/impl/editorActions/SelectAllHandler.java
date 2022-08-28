/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateManagerImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateStateImpl;
import consulo.ui.ex.action.IdeActions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl
public class SelectAllHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  @Override
  public void execute(Editor editor, DataContext dataContext) {
    final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (templateState != null && !templateState.isFinished()) {
      final TextRange range = templateState.getCurrentVariableRange();
      final int caretOffset = editor.getCaretModel().getOffset();
      if (range != null && range.getStartOffset() <= caretOffset && caretOffset <= range.getEndOffset()) {
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        return;
      }
    }
    myOriginalHandler.execute(editor, dataContext);
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_SELECT_ALL;
  }
}
