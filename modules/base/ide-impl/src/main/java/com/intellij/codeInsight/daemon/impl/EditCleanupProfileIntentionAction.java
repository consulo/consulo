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
package com.intellij.codeInsight.daemon.impl;

import consulo.language.editor.intention.IntentionAction;
import com.intellij.codeInspection.actions.CodeCleanupAction;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.codeEditor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import consulo.project.Project;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ui.ProjectInspectionToolsConfigurable;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;

/**
 * Created by anna on 5/13/2014.
 */
public class EditCleanupProfileIntentionAction implements IntentionAction {
  public static final EditCleanupProfileIntentionAction INSTANCE = new EditCleanupProfileIntentionAction();
  private EditCleanupProfileIntentionAction() {}

  @Override
  @Nonnull
  public String getText() {
    return getFamilyName();
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return "Edit cleanup profile settings";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
    final ProjectInspectionToolsConfigurable configurable =
            new ProjectInspectionToolsConfigurable(InspectionProfileManager.getInstance(), profileManager) {
              @Override
              protected boolean acceptTool(InspectionToolWrapper entry) {
                return super.acceptTool(entry) && entry.isCleanupTool();
              }
            };
    ShowSettingsUtil.getInstance().editConfigurable(CodeCleanupAction.CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME,  project, configurable);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
