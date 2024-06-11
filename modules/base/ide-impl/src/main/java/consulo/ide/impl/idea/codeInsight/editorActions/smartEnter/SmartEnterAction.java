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

package consulo.ide.impl.idea.codeInsight.editorActions.smartEnter;

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.EditorAction;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.editorActions.enter.EnterAfterUnmatchedBraceHandler;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateStateImpl;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.language.Language;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.action.SmartEnterProcessor;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author max
 */
public class SmartEnterAction extends EditorAction {
  public SmartEnterAction() {
    super(new Handler());
    setInjectedContext(true);
  }

  private static class Handler extends EditorWriteActionHandler {
    public Handler() {
      super(true);
    }

    @Override
    public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
      return getEnterHandler().isEnabled(editor, caret, dataContext);
    }

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project == null || editor.isOneLineMode()) {
        plainEnter(editor, caret, dataContext);
        return;
      }

      LookupManager.getInstance(project).hideActiveLookup();

      TemplateStateImpl state = TemplateManagerImpl.getTemplateStateImpl(editor);
      if (state != null) {
        state.gotoEnd();
      }

      final int caretOffset = editor.getCaretModel().getOffset();

      PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
      if (psiFile == null) {
        plainEnter(editor, caret, dataContext);
        return;
      }

      if (EnterAfterUnmatchedBraceHandler.isAfterUnmatchedLBrace(editor, caretOffset, psiFile.getFileType())) {
        EditorActionHandler enterHandler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER);
        enterHandler.execute(editor, caret, dataContext);
        return;
      }

      final Language language = PsiUtilBase.getLanguageInEditor(editor, project);
      boolean processed = false;
      if (language != null) {
        final List<SmartEnterProcessor> processors = SmartEnterProcessor.forLanguage(language);
        if (!processors.isEmpty()) {
          for (SmartEnterProcessor processor : processors) {
            if (processor.process(project, editor, psiFile)) {
              processed = true;
              break;
            }
          }
        }
      }
      if (!processed) {
        plainEnter(editor, caret, dataContext);
      }
    }
  }

  public static void plainEnter(Editor editor, Caret caret, DataContext dataContext) {
    getEnterHandler().execute(editor, caret, dataContext);
  }

  private static EditorActionHandler getEnterHandler() {
    return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_START_NEW_LINE);
  }
}

