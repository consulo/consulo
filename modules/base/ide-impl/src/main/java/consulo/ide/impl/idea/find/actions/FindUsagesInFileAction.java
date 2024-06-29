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

package consulo.ide.impl.idea.find.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.fileEditor.FileEditor;
import consulo.find.FindBundle;
import consulo.language.Language;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.findUsage.EmptyFindUsagesProvider;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import jakarta.annotation.Nonnull;

public class FindUsagesInFileAction extends AnAction {

  public FindUsagesInFileAction() {
    setInjectedContext(true);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    if (project == null) return;
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    Editor editor = dataContext.getData(Editor.KEY);

    UsageTarget[] usageTargets = dataContext.getData(UsageView.USAGE_TARGETS_KEY);
    if (usageTargets != null) {
      FileEditor fileEditor = dataContext.getData(FileEditor.KEY);
      if (fileEditor != null) {
        usageTargets[0].findUsagesInEditor(fileEditor);
      }
    }
    else if (editor == null) {
      Messages.showMessageDialog(
        project,
        FindBundle.message("find.no.usages.at.cursor.error"),
        CommonLocalize.titleError().get(),
        UIUtil.getErrorIcon()
      );
    }
    else {
      HintManager.getInstance().showErrorHint(editor, FindBundle.message("find.no.usages.at.cursor.error"));
    }
  }

  @Override
  @RequiredReadAction
  public void update(@Nonnull AnActionEvent event){
    updateFindUsagesAction(event);
  }

  @RequiredReadAction
  private static boolean isEnabled(DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    Editor editor = dataContext.getData(Editor.KEY);
    if (editor == null) {
      UsageTarget[] target = dataContext.getData(UsageView.USAGE_TARGETS_KEY);
      return target != null && target.length > 0;
    }
    else {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        return false;
      }

      Language language = PsiUtilBase.getLanguageInEditor(editor, project);
      if (language == null) {
        language = file.getLanguage();
      }
      return !(FindUsagesProvider.forLanguage(language) instanceof EmptyFindUsagesProvider);
    }
  }

  @RequiredReadAction
  public static void updateFindUsagesAction(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    boolean enabled = isEnabled(dataContext);
    presentation.setVisible(enabled || !ActionPlaces.isPopupPlace(event.getPlace()));
    presentation.setEnabled(enabled);
  }
}
