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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 16, 2002
 * Time: 6:16:33 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.ide.impl.idea.codeInsight.template.impl.actions;

import consulo.language.editor.CodeInsightBundle;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateManagerImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateStateImpl;
import consulo.dataContext.DataContext;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PreviousVariableAction extends EditorAction {
  public PreviousVariableAction() {
    super(new Handler());
    setInjectedContext(true);
  }

  private static class Handler extends EditorActionHandler {

    @Override
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
      final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
      assert templateState != null;
      CommandProcessor.getInstance().setCurrentCommandName(CodeInsightBundle.message("template.previous.variable.command"));
      templateState.previousTab();
    }

    @Override
    protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
      final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
      return templateState != null && !templateState.isFinished();
    }
  }
}