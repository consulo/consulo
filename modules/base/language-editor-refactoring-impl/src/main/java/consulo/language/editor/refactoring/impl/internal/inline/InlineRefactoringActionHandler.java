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
package consulo.language.editor.refactoring.impl.internal.inline;

import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.inline.InlineActionHandler;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Jeka
 * @since 2001-11-21
 */
public class InlineRefactoringActionHandler implements RefactoringActionHandler {
    private static final Logger LOG = Logger.getInstance(InlineRefactoringActionHandler.class);
    private static final LocalizeValue REFACTORING_NAME = RefactoringLocalize.inlineTitle();

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
        LOG.assertTrue(elements.length == 1);
        if (dataContext == null) {
            dataContext = DataManager.getInstance().getDataContext();
        }
        Editor editor = dataContext.getData(Editor.KEY);
        for (InlineActionHandler handler : InlineActionHandler.EP_NAME.getExtensionList()) {
            if (handler.canInlineElement(elements[0])) {
                handler.inlineElement(project, editor, elements[0]);
                return;
            }
        }

        invokeInliner(editor, elements[0]);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

        PsiElement element = dataContext.getData(PsiElement.KEY);
        if (element == null) {
            element = BaseRefactoringAction.getElementAtCaret(editor, file);
        }
        if (element != null) {
            for (InlineActionHandler handler : InlineActionHandler.EP_NAME.getExtensionList()) {
                if (handler.canInlineElementInEditor(element, editor)) {
                    handler.inlineElement(project, editor, element);
                    return;
                }
            }

            if (invokeInliner(editor, element)) {
                return;
            }

            LocalizeValue message =
                RefactoringLocalize.cannotPerformRefactoringWithReason(RefactoringLocalize.errorWrongCaretPositionMethodOrLocalName());
            CommonRefactoringUtil.showErrorHint(project, editor, message.get(), REFACTORING_NAME.get(), null);
        }
    }

    @RequiredUIAccess
    public static boolean invokeInliner(@Nullable Editor editor, PsiElement element) {
        List<InlineHandler> handlers = InlineHandler.forLanguage(element.getLanguage());
        for (InlineHandler handler : handlers) {
            if (GenericInlineHandlerImpl.invoke(element, editor, handler)) {
                return true;
            }
        }
        return false;
    }
}
