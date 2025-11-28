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
package consulo.ide.impl.psi.templateLanguages;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.editor.intention.ErrorQuickFixProvider;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.file.FileViewProvider;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class TemplateLanguageErrorQuickFixProvider implements ErrorQuickFixProvider {
    @Override
    @RequiredReadAction
    public void registerErrorQuickFix(PsiErrorElement errorElement, HighlightInfo.Builder builder) {
        PsiFile psiFile = errorElement.getContainingFile();
        if (psiFile.getViewProvider() instanceof TemplateLanguageFileViewProvider provider
            && psiFile.getLanguage() == provider.getTemplateDataLanguage()) {
            builder.registerFix(createChangeTemplateDataLanguageFix(errorElement));
        }
    }

    public static IntentionAction createChangeTemplateDataLanguageFix(PsiElement errorElement) {
        PsiFile containingFile = errorElement.getContainingFile();
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        final Language language = ((TemplateLanguageFileViewProvider) containingFile.getViewProvider()).getTemplateDataLanguage();
        return new SyntheticIntentionAction() {
            @Nonnull
            @Override
            public LocalizeValue getText() {
                return LanguageLocalize.quickfixChangeTemplateDataLanguageText(language.getDisplayName());
            }

            @Override
            @RequiredUIAccess
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                ShowSettingsUtil.getInstance().showAndSelect(
                    project,
                    TemplateDataLanguageConfigurable.class,
                    configurable -> {
                        if (virtualFile != null) {
                            configurable.selectFile(virtualFile);
                        }
                    }
                );
            }
        };
    }
}
