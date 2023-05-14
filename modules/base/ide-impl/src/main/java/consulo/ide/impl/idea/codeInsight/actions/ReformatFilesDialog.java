/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.actions;

import consulo.language.editor.CodeInsightBundle;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.content.scope.SearchScope;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;

import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.*;

public class ReformatFilesDialog extends DialogWrapper implements ReformatFilesOptions {
  private JPanel myPanel;
  private JCheckBox myOptimizeImports;
  private JCheckBox myOnlyChangedText;
  private JCheckBox myRearrangeEntriesCb;

  private final LastRunReformatCodeOptionsProvider myLastRunSettings;

  public ReformatFilesDialog(@Nonnull Project project, @Nonnull VirtualFile[] files) {
    super(project, true);
    myLastRunSettings = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

    boolean canTargetVcsChanges = FormatChangedTextUtil.hasChanges(files, project);
    myOnlyChangedText.setEnabled(canTargetVcsChanges);
    myOnlyChangedText.setSelected(canTargetVcsChanges && myLastRunSettings.getLastTextRangeType() == VCS_CHANGED_TEXT);
    myOptimizeImports.setSelected(myLastRunSettings.getLastOptimizeImports());
    myRearrangeEntriesCb.setSelected(myLastRunSettings.getLastRearrangeCode());

    setTitle(CodeInsightBundle.message("dialog.reformat.files.title"));
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  public boolean isOptimizeImports(){
    return myOptimizeImports.isSelected();
  }

  @Override
  public TextRangeType getTextRangeType() {
    return myOnlyChangedText.isEnabled() && myOnlyChangedText.isSelected()
           ? VCS_CHANGED_TEXT
           : WHOLE_FILE;
  }

  @Override
  public boolean isRearrangeCode() {
    return myRearrangeEntriesCb.isSelected();
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();
    myLastRunSettings.saveOptimizeImportsState(isOptimizeImports());
    myLastRunSettings.saveRearrangeCodeState(isRearrangeCode());
    if (myOnlyChangedText.isEnabled()) {
      myLastRunSettings.saveProcessVcsChangedTextState(getTextRangeType() == VCS_CHANGED_TEXT);
    }
  }

  @Nullable
  @Override
  public SearchScope getSearchScope() {
    return null;
  }

  @Nullable
  @Override
  public String getFileTypeMask() {
    return null;
  }
}
