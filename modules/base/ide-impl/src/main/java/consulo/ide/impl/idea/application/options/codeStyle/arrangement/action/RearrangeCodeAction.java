/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.codeStyle.arrangement.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.actions.RearrangeCodeProcessor;
import consulo.language.codeStyle.arrangement.Rearranger;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;

/**
 * Arranges content at the target file(s).
 *
 * @author Denis Zhdanov
 * @since 8/30/12 10:01 AM
 */
public class RearrangeCodeAction extends AnAction {

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    PsiFile file = e.getDataContext().getData(CommonDataKeys.PSI_FILE);
    boolean enabled = file != null && Rearranger.forLanguage(file.getLanguage()) != null;
    e.getPresentation().setEnabled(enabled);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (editor == null) {
      return;
    }

    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    Document document = editor.getDocument();
    documentManager.commitDocument(document);

    final PsiFile file = documentManager.getPsiFile(document);
    if (file == null) {
      return;
    }

    SelectionModel model = editor.getSelectionModel();
    if (model.hasSelection()) {
      new RearrangeCodeProcessor(file, model).run();
    }
    else {
      new RearrangeCodeProcessor(file).run();
    }
  }
}
