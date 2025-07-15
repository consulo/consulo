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

import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.file.FileViewProvider;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiManager;
import consulo.language.template.ConfigurableTemplateLanguageFileViewProvider;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class ChangeTemplateDataLanguageAction extends AnAction {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setVisible(false);

    VirtualFile virtualFile = e.getData(VirtualFile.KEY);
    VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
    if (files != null && files.length > 1) {
      virtualFile = null;
    }
    if (virtualFile == null || virtualFile.isDirectory()) return;

    Project project = e.getData(Project.KEY);
    if (project == null) return;

    final FileViewProvider provider = PsiManager.getInstance(project).findViewProvider(virtualFile);
    if (provider instanceof ConfigurableTemplateLanguageFileViewProvider) {
      final TemplateLanguageFileViewProvider viewProvider = (TemplateLanguageFileViewProvider)provider;

      e.getPresentation().setTextValue(LanguageLocalize.quickfixChangeTemplateDataLanguageText(viewProvider.getTemplateDataLanguage().getDisplayName()));
      e.getPresentation().setEnabled(true);
      e.getPresentation().setVisible(true);
    }

  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) return;

    final VirtualFile virtualFile = e.getData(VirtualFile.KEY);
    final TemplateDataLanguageConfigurable configurable = new TemplateDataLanguageConfigurable(project, TemplateDataLanguageMappings.getInstance(project));
    ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
      if (virtualFile != null) {
        configurable.selectFile(virtualFile);
      }
    });
  }
}
