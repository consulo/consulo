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
 * class InlineAction
 * created Aug 28, 2001
 * @author Jeka
 */
package consulo.ide.impl.idea.refactoring.actions;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.lang.refactoring.InlineActionHandler;
import consulo.ide.impl.idea.lang.refactoring.InlineHandler;
import consulo.ide.impl.idea.lang.refactoring.InlineHandlers;
import consulo.ide.impl.idea.refactoring.inline.InlineRefactoringActionHandler;
import consulo.language.Language;
import consulo.language.editor.internal.PsiUtilBase;
import consulo.language.editor.refactoring.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class InlineAction extends BasePlatformRefactoringAction {

  public InlineAction() {
    setInjectedContext(true);
  }

  @Override
  public boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull DataContext context) {
    return hasInlineActionHandler(element, PsiUtilBase.getLanguageInEditor(editor, element.getProject()), editor);
  }

  @Override
  public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
    return elements.length == 1 && hasInlineActionHandler(elements [0], null, null);
  }

  private static boolean hasInlineActionHandler(PsiElement element, @Nullable Language editorLanguage, Editor editor) {
    for(InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
      if (handler.isEnabledOnElement(element, editor)) {
        return true;
      }
    }
    return InlineHandlers.getInlineHandlers(
      editorLanguage != null ? editorLanguage :element.getLanguage()
    ).size() > 0;
  }

  @Override
  protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
    return new InlineRefactoringActionHandler();
  }

  @Override
  protected RefactoringActionHandler getHandler(@Nonnull Language language, PsiElement element) {
    RefactoringActionHandler handler = super.getHandler(language, element);
    if (handler != null) return handler;
    List<InlineHandler> handlers = InlineHandlers.getInlineHandlers(language);
    return handlers.isEmpty() ? null : new InlineRefactoringActionHandler();
  }

  @Override
  protected boolean isAvailableForLanguage(Language language) {
    for(InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
      if (handler.isEnabledForLanguage(language)) {
        return true;
      }
    }
    return InlineHandlers.getInlineHandlers(language).size() > 0;
  }
}
