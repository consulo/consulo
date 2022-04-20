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

/**
 * created at Nov 21, 2001
 * @author Jeka
 */
package com.intellij.refactoring.inline;

import consulo.dataContext.DataManager;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.lang.refactoring.InlineHandler;
import com.intellij.lang.refactoring.InlineHandlers;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.component.extension.Extensions;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public class InlineRefactoringActionHandler implements RefactoringActionHandler {
  private static final Logger LOG = Logger.getInstance(InlineRefactoringActionHandler.class);
  private static final String REFACTORING_NAME = RefactoringBundle.message("inline.title");

  @Override
  public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    LOG.assertTrue(elements.length == 1);
    if (dataContext == null) {
      dataContext = DataManager.getInstance().getDataContext();
    }
    final Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    for(InlineActionHandler handler: Extensions.getExtensions(InlineActionHandler.EP_NAME)) {
      if (handler.canInlineElement(elements[0])) {
        handler.inlineElement(project, editor, elements [0]);
        return;
      }
    }

    invokeInliner(editor, elements[0]);
  }

  @Override
  public void invoke(@Nonnull final Project project, Editor editor, PsiFile file, DataContext dataContext) {
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

    PsiElement element = dataContext.getData(LangDataKeys.PSI_ELEMENT);
    if (element == null) {
      element = BaseRefactoringAction.getElementAtCaret(editor, file);
    }
    if (element != null) {
      for(InlineActionHandler handler: Extensions.getExtensions(InlineActionHandler.EP_NAME)) {
        if (handler.canInlineElementInEditor(element, editor)) {
          handler.inlineElement(project, editor, element);
          return;
        }
      }

      if (invokeInliner(editor, element)) return;

      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.method.or.local.name"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, null);
    }
  }

  public static boolean invokeInliner(@Nullable Editor editor, PsiElement element) {
    final List<InlineHandler> handlers = InlineHandlers.getInlineHandlers(element.getLanguage());
    for (InlineHandler handler : handlers) {
      if (GenericInlineHandler.invoke(element, editor, handler)) {
        return true;
      }
    }
    return false;
  }
}
