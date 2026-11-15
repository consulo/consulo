/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.ElementsHandler;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class BasePlatformRefactoringAction extends BaseRefactoringAction {
    private Boolean myHidden = null;

    protected BasePlatformRefactoringAction() {
    }

    protected BasePlatformRefactoringAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    @Override
    @RequiredReadAction
    protected final RefactoringActionHandler getHandler(DataContext dataContext) {
        PsiElement element = null;
        Editor editor = dataContext.getData(Editor.KEY);
        PsiFile file = dataContext.getData(PsiFile.KEY);
        if (editor != null && file != null) {
            element = getElementAtCaret(editor, file);
            if (element != null) {
                RefactoringActionHandler handler = getHandler(element.getLanguage(), element);
                if (handler != null) {
                    return handler;
                }
            }
        }

        PsiElement referenced = dataContext.getData(PsiElement.KEY);
        if (referenced != null) {
            RefactoringActionHandler handler = getHandler(referenced.getLanguage(), referenced);
            if (handler != null) {
                return handler;
            }
        }

        PsiElement[] psiElements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
        if (psiElements != null && psiElements.length > 1) {
            RefactoringActionHandler handler = getHandler(psiElements[0].getLanguage(), psiElements[0]);
            if (handler != null && isEnabledOnElements(psiElements)) {
                return handler;
            }
        }

        if (element == null) {
            element = referenced;
        }

        if (element == null) {
            return null;
        }

        Language[] languages = dataContext.getData(LangDataKeys.CONTEXT_LANGUAGES);
        if (languages != null) {
            for (Language language : languages) {
                RefactoringActionHandler handler = getHandler(language, element);
                if (handler != null) {
                    return handler;
                }
            }
        }

        return null;
    }

    protected @Nullable RefactoringActionHandler getHandler(Language language, @Nullable PsiElement element) {
        if (element == null) {
            // a Language.ANY provider decides per element, so it cannot answer without one
            return getRefactoringHandler(RefactoringSupportProvider.forLanguage(language));
        }
        for (RefactoringSupportProvider provider : allForLanguageOrAny(language)) {
            if (provider.isAvailable(element)) {
                RefactoringActionHandler handler = getRefactoringHandler(provider, element);
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    /**
     * The providers of the language, then the {@link Language#ANY} ones. A language without a provider gets the default one of
     * {@link RefactoringSupportProvider#forLanguage} first (some actions return a handler for any provider).
     */
    private static List<RefactoringSupportProvider> allForLanguageOrAny(Language language) {
        List<RefactoringSupportProvider> providers = RefactoringSupportProvider.allForLanguage(language);
        List<RefactoringSupportProvider> anyLanguageProviders = anyLanguageProviders(language);
        if (providers.isEmpty()) {
            providers = List.of(RefactoringSupportProvider.forLanguage(language));
        }
        if (anyLanguageProviders.isEmpty()) {
            return providers;
        }
        List<RefactoringSupportProvider> result = new ArrayList<>(providers.size() + anyLanguageProviders.size());
        result.addAll(providers);
        result.addAll(anyLanguageProviders);
        return result;
    }

    @Override
    @RequiredReadAction
    protected boolean isAvailableOnElementInEditorAndFile(
        PsiElement element,
        Editor editor,
        PsiFile file,
        DataContext context
    ) {
        return getHandler(context) != null;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        RefactoringSupportProvider refactoringSupportProvider = RefactoringSupportProvider.forLanguage(language);
        // any language mean default provider
        return refactoringSupportProvider.getLanguage() != Language.ANY;
    }

    @Override
    @RequiredReadAction
    protected boolean isAvailableForAnyLanguage(PsiFile file) {
        for (RefactoringSupportProvider provider : anyLanguageProviders()) {
            if (getRefactoringHandler(provider) != null && provider.isAvailable(file)) {
                return true;
            }
        }
        return false;
    }

    private static List<RefactoringSupportProvider> anyLanguageProviders() {
        return RefactoringSupportProvider.allForLanguage(Language.ANY);
    }

    private static List<RefactoringSupportProvider> anyLanguageProviders(Language language) {
        return language == Language.ANY ? List.of() : anyLanguageProviders();
    }

    @Override
    @RequiredReadAction
    protected boolean isEnabledOnElements(PsiElement[] elements) {
        if (elements.length > 0) {
            Language language = elements[0].getLanguage();
            return getHandler(language, elements[0]) instanceof ElementsHandler elementsHandler
                && elementsHandler.isEnabledOnElements(elements);
        }
        return false;
    }

    protected abstract @Nullable RefactoringActionHandler getRefactoringHandler(RefactoringSupportProvider provider);

    protected @Nullable RefactoringActionHandler getRefactoringHandler(RefactoringSupportProvider provider, PsiElement element) {
        return getRefactoringHandler(provider);
    }

    @Override
    protected boolean isHidden() {
        if (myHidden == null) {
            myHidden = calcHidden();
        }
        return myHidden;
    }

    private boolean calcHidden() {
        for (Language l : Language.getRegisteredLanguages()) {
            if (isAvailableForLanguage(l)) {
                return false;
            }
        }
        // a Language.ANY provider (e.g. an LSP server) serves any language, decided per file
        for (RefactoringSupportProvider provider : anyLanguageProviders()) {
            if (getRefactoringHandler(provider) != null) {
                return false;
            }
        }
        return true;
    }
}
