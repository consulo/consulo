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
package consulo.language.editor.refactoring.impl.internal.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.action.BasePlatformRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.impl.internal.inline.InlineRefactoringActionHandler;
import consulo.language.editor.refactoring.inline.InlineActionHandler;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author Jeka
 * @since 2001-08-28
 */
@ActionImpl(id = "Inline")
public class InlineAction extends BasePlatformRefactoringAction {
    
    private final Application myApplication;

    @Inject
    public InlineAction(Application application) {
        super(ActionLocalize.actionInlineText(), ActionLocalize.actionInlineDescription());
        setInjectedContext(true);
        myApplication = application;
    }

    @Override
    public boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    @RequiredReadAction
    protected boolean isAvailableOnElementInEditorAndFile(
        PsiElement element,
        Editor editor,
        PsiFile file,
        DataContext context
    ) {
        return hasInlineActionHandler(element, PsiUtilBase.getLanguageInEditor(editor, element.getProject()), editor);
    }

    @Override
    @RequiredReadAction
    public boolean isEnabledOnElements(PsiElement[] elements) {
        return elements.length == 1 && hasInlineActionHandler(elements[0], null, null);
    }

    @RequiredReadAction
    private boolean hasInlineActionHandler(PsiElement element, @Nullable Language editorLanguage, Editor editor) {
        return myApplication.getExtensionPoint(InlineActionHandler.class)
            .anyMatchSafe(handler -> handler.isEnabledOnElement(element, editor))
            || !InlineHandler.forLanguage(editorLanguage != null ? editorLanguage : element.getLanguage()).isEmpty();
    }

    @Override
    protected RefactoringActionHandler getRefactoringHandler(RefactoringSupportProvider provider) {
        return new InlineRefactoringActionHandler();
    }

    @Override
    protected RefactoringActionHandler getHandler(Language language, PsiElement element) {
        RefactoringActionHandler handler = super.getHandler(language, element);
        if (handler != null) {
            return handler;
        }
        List<InlineHandler> handlers = InlineHandler.forLanguage(language);
        return handlers.isEmpty() ? null : new InlineRefactoringActionHandler();
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return myApplication.getExtensionPoint(InlineActionHandler.class)
            .anyMatchSafe(handler -> handler.isEnabledForLanguage(language))
            || InlineHandler.forLanguage(language).size() > 0;
    }
}
