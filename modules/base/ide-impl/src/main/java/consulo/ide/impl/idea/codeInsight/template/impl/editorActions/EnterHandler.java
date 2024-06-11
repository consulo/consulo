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
package consulo.ide.impl.idea.codeInsight.template.impl.editorActions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.editorActions.BaseEnterHandler;
import consulo.language.editor.impl.internal.template.TemplateSettingsImpl;
import consulo.language.editor.template.TemplateManager;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "templateEnter", order = "before editorEnter")
public class EnterHandler extends BaseEnterHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  @Inject
  public EnterHandler() {
    super(true);
  }

  @Override
  public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
    return myOriginalHandler.isEnabled(editor, caret, dataContext);
  }

  @RequiredWriteAction
  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    final Project project = editor.getProject();
    if (project != null && TemplateManager.getInstance(project).startTemplate(editor, TemplateSettingsImpl.ENTER_CHAR)) {
      return;
    }

    if (myOriginalHandler != null) {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_ENTER;
  }
}
