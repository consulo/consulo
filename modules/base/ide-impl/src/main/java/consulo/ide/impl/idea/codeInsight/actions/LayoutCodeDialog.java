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
import consulo.application.HelpManager;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.arrangement.Rearranger;
import consulo.language.file.light.LightVirtualFile;
import consulo.versionControlSystem.FormatChangedTextUtil;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;

public class LayoutCodeDialog extends DialogWrapper {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final PsiFile myFile;

  private final boolean myTextSelected;

  private final String myHelpId;
  private final LastRunReformatCodeOptionsProvider myLastRunOptions;

  private JPanel myButtonsPanel;

  private JCheckBox myOptimizeImportsCb;
  private JCheckBox myRearrangeCodeCb;

  private JRadioButton myOnlyVCSChangedTextRb;
  private JRadioButton mySelectedTextRadioButton;
  private JRadioButton myWholeFileRadioButton;

  private JPanel myActionsPanel;
  private JPanel myScopePanel;
  private JLabel myOptionalLabel;

  private LayoutCodeOptions myRunOptions;

  public LayoutCodeDialog(@Nonnull Project project,
                          @Nonnull PsiFile file,
                          boolean textSelected,
                          String helpId) {
    super(project, true);
    myFile = file;
    myProject = project;
    myTextSelected = textSelected;
    myHelpId = helpId;

    myLastRunOptions = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
    myRunOptions = createOptionsBundledOnDialog();

    setOKButtonText(CodeInsightBundle.message("reformat.code.accept.button.text"));
    setTitle("Reformat File: " + file.getName());

    init();
  }

  protected void init() {
    super.init();

    setUpActions();
    setUpTextRangeMode();
  }


  private void setUpTextRangeMode() {
    mySelectedTextRadioButton.setEnabled(myTextSelected);
    if (!myTextSelected) {
      mySelectedTextRadioButton.setToolTipText("No text selected in editor");
    }

    boolean fileHasChanges = FormatChangedTextUtil.hasChanges(myFile);
    if (myFile.getVirtualFile() instanceof LightVirtualFile) {
      myOnlyVCSChangedTextRb.setVisible(false);
    }
    else {
      myOnlyVCSChangedTextRb.setEnabled(fileHasChanges);
      if (!fileHasChanges) {
        String hint = getChangesNotAvailableHint();
        if (hint != null) myOnlyVCSChangedTextRb.setToolTipText(hint);
      }
    }

    myWholeFileRadioButton.setEnabled(true);

    if (myTextSelected) {
      mySelectedTextRadioButton.setSelected(true);
    }
    else {
      boolean lastRunProcessedChangedText = myLastRunOptions.getLastTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;
      if (lastRunProcessedChangedText && fileHasChanges) {
        myOnlyVCSChangedTextRb.setSelected(true);
      }
      else {
        myWholeFileRadioButton.setSelected(true);
      }
    }
  }

  private void setUpActions() {
    boolean canOptimizeImports = !ImportOptimizer.forFile(myFile).isEmpty();
    myOptimizeImportsCb.setVisible(canOptimizeImports);
    if (canOptimizeImports) {
      myOptimizeImportsCb.setSelected(myLastRunOptions.getLastOptimizeImports());
    }

    boolean canRearrangeCode = Rearranger.forLanguage(myFile.getLanguage()) != null;
    myRearrangeCodeCb.setVisible(canRearrangeCode);
    if (canRearrangeCode) {
      myRearrangeCodeCb.setSelected(myLastRunOptions.isRearrangeCode(myFile.getLanguage()));
    }

    myOptionalLabel.setVisible(canOptimizeImports || canRearrangeCode);
  }

  @Nullable
  private String getChangesNotAvailableHint() {
    if (!VcsUtil.isFileUnderVcs(myProject, VcsUtil.getFilePath(myFile.getVirtualFile()))) {
      return "File not under VCS root";
    }
    else if (!FormatChangedTextUtil.hasChanges(myFile)) {
      return "File was not changed since last revision";
    }
    return null;
  }

  private void saveCurrentConfiguration() {
    if (myOptimizeImportsCb.isEnabled()) {
      myLastRunOptions.saveOptimizeImportsState(myRunOptions.isOptimizeImports());
    }
    if (myRearrangeCodeCb.isEnabled()) {
      myLastRunOptions.saveRearrangeState(myFile.getLanguage(), myRunOptions.isRearrangeCode());
    }

    if (!mySelectedTextRadioButton.isSelected() && myOnlyVCSChangedTextRb.isEnabled()) {
      myLastRunOptions.saveProcessVcsChangedTextState(myOnlyVCSChangedTextRb.isSelected());
    }
  }

  @Nonnull
  private LayoutCodeOptions createOptionsBundledOnDialog() {
    return new LayoutCodeOptions() {
      @Override
      public TextRangeType getTextRangeType() {
        if (myOnlyVCSChangedTextRb.isSelected()) {
          return TextRangeType.VCS_CHANGED_TEXT;
        }
        if (mySelectedTextRadioButton.isSelected()) {
          return TextRangeType.SELECTED_TEXT;
        }
        return TextRangeType.WHOLE_FILE;
      }

      @Override
      public boolean isRearrangeCode() {
        return myRearrangeCodeCb.isEnabled() && myRearrangeCodeCb.isSelected();
      }

      @Override
      public boolean isOptimizeImports() {
        return myOptimizeImportsCb.isEnabled() && myOptimizeImportsCb.isSelected();
      }
    };
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myButtonsPanel;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(myHelpId);
  }

  @Override
  protected void doOKAction() {
    saveCurrentConfiguration();
    super.doOKAction();
  }

  public LayoutCodeOptions getRunOptions() {
    return myRunOptions;
  }

}
