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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.application.CommonBundle;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.ModifiableModel;
import consulo.application.AllIcons;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.component.util.Iconable;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DisableInspectionToolAction implements IntentionAction, Iconable {
  private final String myToolId;
  public static final String NAME = InspectionsBundle.message("disable.inspection.action.name");

  public DisableInspectionToolAction(LocalInspectionTool tool) {
    myToolId = tool.getShortName();
  }

  public DisableInspectionToolAction(final HighlightDisplayKey key) {
    myToolId = key.toString();
  }

  @Override
  @Nonnull
  public String getText() {
    return NAME;
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return NAME;
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(file.getProject());
    InspectionProfile inspectionProfile = profileManager.getInspectionProfile();
    ModifiableModel model = inspectionProfile.getModifiableModel();
    model.disableTool(myToolId, file);
    try {
      model.commit();
    }
    catch (IOException e) {
      Messages.showErrorDialog(project, e.getMessage(), CommonBundle.getErrorTitle());
    }
    DaemonCodeAnalyzer.getInstance(project).restart();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public Image getIcon(int flags) {
    return AllIcons.Actions.Cancel;
  }
}
