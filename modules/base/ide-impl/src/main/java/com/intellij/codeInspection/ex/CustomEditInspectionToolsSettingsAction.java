package com.intellij.codeInspection.ex;

import consulo.language.editor.highlight.HighlightDisplayKey;
import consulo.language.editor.intention.IntentionAction;
import consulo.editor.Editor;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.component.util.Iconable;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
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
