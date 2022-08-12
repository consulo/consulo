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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.ErrorQuickFixProvider;
import consulo.language.editor.intention.QuickFixAction;
import consulo.language.LangBundle;
import consulo.codeEditor.Editor;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class TemplateLanguageErrorQuickFixProvider implements ErrorQuickFixProvider{

  @Override
  public void registerErrorQuickFix(final PsiErrorElement errorElement, final HighlightInfo highlightInfo) {
    final PsiFile psiFile = errorElement.getContainingFile();
    final FileViewProvider provider = psiFile.getViewProvider();
    if (!(provider instanceof TemplateLanguageFileViewProvider)) return;
    if (psiFile.getLanguage() != ((TemplateLanguageFileViewProvider) provider).getTemplateDataLanguage()) return;

    QuickFixAction.registerQuickFixAction(highlightInfo, createChangeTemplateDataLanguageFix(errorElement));

  }

  public static IntentionAction createChangeTemplateDataLanguageFix(final PsiElement errorElement) {
    final PsiFile containingFile = errorElement.getContainingFile();
    final VirtualFile virtualFile = containingFile.getVirtualFile();
    final Language language = ((TemplateLanguageFileViewProvider) containingFile.getViewProvider()).getTemplateDataLanguage();
    return new IntentionAction() {

      @Override
      @Nonnull
      public String getText() {
        return LangBundle.message("quickfix.change.template.data.language.text", language.getDisplayName());
      }

      @Override
      @Nonnull
      public String getFamilyName() {
        return getText();
      }

      @Override
      public boolean isAvailable(@Nonnull final Project project, final Editor editor, final PsiFile file) {
        return true;
      }

      @RequiredUIAccess
      @Override
      public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        final TemplateDataLanguageConfigurable configurable = new TemplateDataLanguageConfigurable(project, TemplateDataLanguageMappings.getInstance(project));
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, new Runnable() {
          @Override
          public void run() {
            if (virtualFile != null) {
              configurable.selectFile(virtualFile);
            }
          }
        });
      }

      @Override
      public boolean startInWriteAction() {
        return false;
      }
    };
  }

}
