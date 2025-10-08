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

import consulo.application.AllIcons;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.ui.ProjectInspectionToolsConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author anna
 * @since 2005-02-07
 */
public class EditInspectionToolsSettingsAction implements IntentionAction, Iconable, HighPriorityAction, SyntheticIntentionAction {
  private final String myShortName;

  public EditInspectionToolsSettingsAction(@Nonnull LocalInspectionTool tool) {
    myShortName = tool.getShortName();
  }

  public EditInspectionToolsSettingsAction(@Nonnull HighlightDisplayKey key) {
    myShortName = key.toString();
  }

  @Override
  @Nonnull
  public LocalizeValue getText() {
    return InspectionLocalize.editOptionsOfReporterInspectionText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  @RequiredUIAccess
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(file.getProject());
    InspectionProfile inspectionProfile = projectProfileManager.getInspectionProfile();
    editToolSettings(project,
                     inspectionProfile, true,
                     myShortName);
  }

  @RequiredUIAccess
  public AsyncResult<Void> editToolSettings(Project project,
                                            InspectionProfileImpl inspectionProfile,
                                            boolean canChooseDifferentProfiles) {
    return editToolSettings(project,
                            inspectionProfile,
                            canChooseDifferentProfiles,
                            myShortName);
  }

  @RequiredUIAccess
  public static AsyncResult<Void> editToolSettings(Project project,
                                                   InspectionProfile inspectionProfile,
                                                   boolean canChooseDifferentProfile,
                                                   String selectedToolShortName) {
    ShowSettingsUtil settingsUtil = ShowSettingsUtil.getInstance();

    return settingsUtil.showAndSelect(project, ProjectInspectionToolsConfigurable.class, errorsConfigurable -> {
      errorsConfigurable.selectProfile(inspectionProfile);

      SwingUtilities.invokeLater(() -> errorsConfigurable.selectInspectionTool(selectedToolShortName));
    });
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public Image getIcon(int flags) {
    return AllIcons.General.Settings;
  }
}
