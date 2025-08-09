/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.impl.internal.changeSignature;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.changeSignature.inplace.InplaceChangeSignature;
import consulo.ui.ex.action.IdeActions;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "changeSignatureEscape", order = "before hide-search")
public class EscapeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  @Override
  public void execute(Editor editor, DataContext dataContext) {
    InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(editor);
    if (currentRefactoring != null) {
      currentRefactoring.cancel();
      return;
    }

    if (myOriginalHandler.isEnabled(editor, dataContext)) {
      myOriginalHandler.execute(editor, dataContext);
    }
  }

  @Override
  public boolean isEnabled(Editor editor, DataContext dataContext) {
    InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(editor);
    if (currentRefactoring != null) {
      return true;
    }
    return myOriginalHandler.isEnabled(editor, dataContext);
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_ESCAPE;
  }
}
