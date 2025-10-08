package consulo.ide.impl.idea.codeInspection.ex;

import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

public class CustomEditInspectionToolsSettingsAction implements SyntheticIntentionAction, Iconable {
  private final EditInspectionToolsSettingsAction myEditInspectionToolsSettingsAction;   // we delegate due to priority
  private final LocalizeValue myText;

  public CustomEditInspectionToolsSettingsAction(HighlightDisplayKey displayKey, LocalizeValue text) {
    myEditInspectionToolsSettingsAction = new EditInspectionToolsSettingsAction(displayKey);
    myText = text;
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return myText;
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
