// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import consulo.language.editor.CodeInsightBundle;
import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.TemplateActionContext;
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.codeEditor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.language.psi.PsiFile;
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
    List<TemplateImpl> templates = TemplateManagerImpl.listApplicableTemplates(templateActionContext);
    if (templates.isEmpty() && customTemplates.isEmpty()) {
      return Collections.emptyList();
    }

    List<AnAction> group = new ArrayList<>();

    for (TemplateImpl template : templates) {
      group.add(new InvokeTemplateAction(template, editor, file.getProject(), usedMnemonicsSet, () -> SurroundWithLogger.logTemplate(template, file.getLanguage(), file.getProject())));
    }

    for (CustomLiveTemplate customTemplate : customTemplates) {
      group.add(new WrapWithCustomTemplateAction(customTemplate, editor, file, usedMnemonicsSet, () -> SurroundWithLogger.
              logCustomTemplate(customTemplate, file.getLanguage(), file.getProject())));
    }
    return group;
  }
}
