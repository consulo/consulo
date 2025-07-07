/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.impl.internal.generation;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.FileDocumentManager;
import consulo.language.Language;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.LanguageCodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.language.extension.ByLanguageValue;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-07-17
 */
public abstract class MethodsBasedAction<T extends LanguageCodeInsightActionHandler> extends BaseCodeInsightAction implements CodeInsightActionHandler {
    private final Application myApplication;
    private final Class<T> myHandlerType;
    private final ExtensionPointCacheKey<T, ByLanguageValue<T>> myExtensionCacheKey;

    public MethodsBasedAction(
        Application application,
        Class<T> handlerType,
        ExtensionPointCacheKey<T, ByLanguageValue<T>> extensionCacheKey
    ) {
        myApplication = application;
        myHandlerType = handlerType;
        myExtensionCacheKey = extensionCacheKey;
    }

    @Override
    protected final boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
        T codeInsightActionHandler =
            myApplication.getExtensionPoint(myHandlerType).getOrBuildCache(myExtensionCacheKey).get(language);
        return codeInsightActionHandler != null && codeInsightActionHandler.isValidFor(editor, file);
    }

    @Override
    public final void update(@Nonnull AnActionEvent event) {
        if (myApplication.getExtensionPoint(myHandlerType).hasAnyExtensions()) {
            event.getPresentation().setVisible(true);
            super.update(event);
        }
        else {
            event.getPresentation().setVisible(false);
        }
    }

    @RequiredUIAccess
    @Override
    public final void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        if (!LanguageEditorUtil.checkModificationAllowed(editor)) {
            return;
        }

        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }

        Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
        ExtensionPoint<T> extensionPoint = project.getApplication().getExtensionPoint(myHandlerType);
        LanguageCodeInsightActionHandler codeInsightActionHandler = extensionPoint.getOrBuildCache(myExtensionCacheKey).get(language);

        if (codeInsightActionHandler != null) {
            codeInsightActionHandler.invoke(project, editor, file);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Nonnull
    @Override
    protected final CodeInsightActionHandler getHandler() {
        return this;
    }
}
