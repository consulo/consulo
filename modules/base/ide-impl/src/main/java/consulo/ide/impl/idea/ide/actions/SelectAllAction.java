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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.IdeBundle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.ide.impl.idea.openapi.editor.actions.TextComponentEditorAction;
import consulo.application.dumb.DumbAware;

public class SelectAllAction extends TextComponentEditorAction implements DumbAware {
  public SelectAllAction() {
    super(new Handler());
  }

  private static class Handler extends EditorActionHandler {
    @Override
    public void execute(final Editor editor, DataContext dataContext) {
      CommandProcessor processor = CommandProcessor.getInstance();
      processor.executeCommand(dataContext.getData(CommonDataKeys.PROJECT), new Runnable() {
        public void run() {
          editor.getSelectionModel().setSelection(0, editor.getDocument().getTextLength());
        }
      }, IdeBundle.message("command.select.all"), null);
    }
  }

  @RequiredUIAccess
  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    Editor editor = TextComponentEditorAction.getEditorFromContext(event.getDataContext());
    presentation.setEnabled(editor != null);
  }
}
