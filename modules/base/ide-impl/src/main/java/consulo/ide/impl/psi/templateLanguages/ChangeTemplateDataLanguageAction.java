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

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.language.template.ConfigurableTemplateLanguageFileViewProvider;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.psi.PsiManager;
import consulo.language.file.FileViewProvider;
import consulo.language.LangBundle;

/**
 * @author peter
 */
public class ChangeTemplateDataLanguageAction extends AnAction {
  @Override
  public void update(final AnActionEvent e) {
    e.getPresentation().setVisible(false);

    VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    if (files != null && files.length > 1) {
      virtualFile = null;
    }
    if (virtualFile == null || virtualFile.isDirectory()) return;

    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    final FileViewProvider provider = PsiManager.getInstance(project).findViewProvider(virtualFile);
    if (provider instanceof ConfigurableTemplateLanguageFileViewProvider) {
      final TemplateLanguageFileViewProvider viewProvider = (TemplateLanguageFileViewProvider)provider;

      e.getPresentation().setText(LangBundle.message("quickfix.change.template.data.language.text", viewProvider.getTemplateDataLanguage().getDisplayName()));
      e.getPresentation().setEnabled(true);
      e.getPresentation().setVisible(true);
    }

  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    final TemplateDataLanguageConfigurable configurable = new TemplateDataLanguageConfigurable(project);
    ShowSettingsUtil.getInstance().editConfigurable(project, configurable, new Runnable() {
      @Override
      public void run() {
        if (virtualFile != null) {
          configurable.selectFile(virtualFile);
        }
      }
    });
  }


}
