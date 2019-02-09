package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

public class CustomEditInspectionToolsSettingsAction implements IntentionAction, Iconable {
  private final EditInspectionToolsSettingsAction myEditInspectionToolsSettingsAction;   // we delegate due to priority
  private final Computable<String> myText;

  public CustomEditInspectionToolsSettingsAction(HighlightDisplayKey displayKey, Computable<String> text) {
    myEditInspectionToolsSettingsAction = new EditInspectionToolsSettingsAction(displayKey);
    myText = text;
  }

  @Nonnull
  @Override
  public String getText() {
    return myText.compute();
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return myEditInspectionToolsSettingsAction.getFamilyName();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return myEditInspectionToolsSettingsAction.isAvailable(project, editor, file);
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    myEditInspectionToolsSettingsAction.invoke(project, editor, file);
  }

  @Override
  public boolean startInWriteAction() {
    return myEditInspectionToolsSettingsAction.startInWriteAction();
  }

  @Override
  public Image getIcon(@IconFlags int flags) {
    return myEditInspectionToolsSettingsAction.getIcon(flags);
  }
}
