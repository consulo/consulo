/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class HomeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  @Override
  public void doExecute(Editor editor, Caret caret, DataContext dataContext) {
    LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup == null || !lookup.isFocused()) {
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    lookup.markSelectionTouched();
    lookup.moveHome();
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_MOVE_LINE_START;
  }
}
