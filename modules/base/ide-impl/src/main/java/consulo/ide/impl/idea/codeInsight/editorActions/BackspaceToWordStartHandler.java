/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.IdeActions;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl(order = "first")
public class BackspaceToWordStartHandler extends BackspaceHandler {
  @RequiredWriteAction
  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    if (!handleBackspace(editor, caret, dataContext, true)) {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_DELETE_TO_WORD_START;
  }
}
