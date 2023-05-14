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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInspection.actions.CodeCleanupAction;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.ui.ProjectInspectionToolsConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * Created by anna on 5/13/2014.
 */
public class EditCleanupProfileIntentionAction implements IntentionAction {
  public static final EditCleanupProfileIntentionAction INSTANCE = new EditCleanupProfileIntentionAction();

  private EditCleanupProfileIntentionAction() {
  }

  @Override
  @Nonnull
  public String getText() {
    return "Edit cleanup profile settings";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
    final ProjectInspectionToolsConfigurable configurable = new ProjectInspectionToolsConfigurable(InspectionProfileManager.getInstance(), profileManager) {
      @Override
      protected boolean acceptTool(InspectionToolWrapper entry) {
        return super.acceptTool(entry) && entry.isCleanupTool();
      }
    };
    ShowSettingsUtil.getInstance().editConfigurable(CodeCleanupAction.CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME, project, configurable);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
