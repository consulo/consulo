// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.codeInsight.generation.surroundWith.SurroundWithHandler;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.template.CustomLiveTemplate;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

import javax.annotation.Nonnull;
import java.util.*;

public class SurroundWithTemplateHandler implements CodeInsightActionHandler {
  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
    if (!EditorModificationUtil.checkModificationAllowed(editor)) return;
    if (!editor.getSelectionModel().hasSelection()) {
      SurroundWithHandler.selectLogicalLineContentsAtCaret(editor);
      if (!editor.getSelectionModel().hasSelection()) return;
    }

    List<AnAction> group = createActionGroup(editor, file, new HashSet<>());
    if (group.isEmpty()) {
      HintManager.getInstance().showErrorHint(editor, CodeInsightBundle.message("templates.surround.no.defined"));
      return;
    }


    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(CodeInsightBundle.message("templates.select.template.chooser.title"), new DefaultActionGroup(group),
                                                                          DataManager.getInstance().getDataContext(editor.getContentComponent()), JBPopupFactory.ActionSelectionAid.MNEMONICS, false);

    editor.showPopupInBestPositionFor(popup);
  }

  @Nonnull
  public static List<AnAction> createActionGroup(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull Set<Character> usedMnemonicsSet) {
    TemplateActionContext templateActionContext = TemplateActionContext.surrounding(file, editor);
    List<CustomLiveTemplate> customTemplates = TemplateManagerImpl.listApplicableCustomTemplates(templateActionContext);
    List<? extends Template> templates = TemplateManager.getInstance(file.getProject()).listApplicableTemplates(templateActionContext);
    if (templates.isEmpty() && customTemplates.isEmpty()) {
      return Collections.emptyList();
    }

    List<AnAction> group = new ArrayList<>();

    for (Template template : templates) {
      group.add(new InvokeTemplateAction(template, editor, file.getProject(), usedMnemonicsSet, () -> SurroundWithLogger.logTemplate(template, file.getLanguage(), file.getProject())));
    }

    for (CustomLiveTemplate customTemplate : customTemplates) {
      group.add(new WrapWithCustomTemplateAction(customTemplate, editor, file, usedMnemonicsSet, () -> SurroundWithLogger.
              logCustomTemplate(customTemplate, file.getLanguage(), file.getProject())));
    }
    return group;
  }
}
