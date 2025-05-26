/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.application.AllIcons;
import consulo.application.HelpManager;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;
import consulo.find.FindSettings;
import consulo.find.ui.ScopeChooserCombo;
import consulo.ide.impl.idea.find.impl.FindDialog;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

/**
 * @author max
 */
public class LayoutProjectCodeDialog extends DialogWrapper implements ReformatFilesOptions {
  private static @NonNls final String HELP_ID = "Reformat Code on Directory Dialog";

  private final Project myProject;
  private final String  myText;
  private final boolean myEnableOnlyVCSChangedTextCb;
  private final LastRunReformatCodeOptionsProvider myLastRunOptions;

  private JLabel myTitle;
  protected JCheckBox myIncludeSubdirsCb;

  private JCheckBox myUseScopeFilteringCb;
  private ScopeChooserCombo myScopeCombo;

  private JCheckBox myEnableFileNameFilterCb;
  private ComboBox myFileFilter;

  private JCheckBox myCbOptimizeImports;
  private JCheckBox myCbRearrangeEntries;
  private JCheckBox myCbOnlyVcsChangedRegions;

  private JPanel myWholePanel;
  private JPanel myOptionsPanel;
  private JPanel myFiltersPanel;
  private JLabel myMaskWarningLabel;

  public LayoutProjectCodeDialog(@Nonnull Project project,
                                 @Nonnull String title,
                                 @Nonnull String text,
                                 boolean enableOnlyVCSChangedTextCb)
  {
    super(project, false);
    myText = text;
    myProject = project;
    myEnableOnlyVCSChangedTextCb = enableOnlyVCSChangedTextCb;
    myLastRunOptions = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

    setOKButtonText(CodeInsightLocalize.reformatCodeAcceptButtonText());
    setTitle(title);
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    myTitle.setText(myText);
    myOptionsPanel.setBorder(IdeBorderFactory.createTitledBorder(CodeInsightLocalize.reformatDirectoryDialogOptions().get()));
    myFiltersPanel.setBorder(IdeBorderFactory.createTitledBorder(CodeInsightLocalize.reformatDirectoryDialogFilters().get()));

    myMaskWarningLabel.setIcon(TargetAWT.to(AllIcons.General.Warning));
    myMaskWarningLabel.setVisible(false);

    myIncludeSubdirsCb.setVisible(shouldShowIncludeSubdirsCb());

    initFileTypeFilter();
    initScopeFilter();

    restoreCbsStates();
    return myWholePanel;
  }

  private void restoreCbsStates() {
    myCbOptimizeImports.setSelected(myLastRunOptions.getLastOptimizeImports());
    myCbRearrangeEntries.setSelected(myLastRunOptions.getLastRearrangeCode());
    myCbOnlyVcsChangedRegions.setEnabled(myEnableOnlyVCSChangedTextCb);
    myCbOnlyVcsChangedRegions.setSelected(
            myEnableOnlyVCSChangedTextCb && myLastRunOptions.getLastTextRangeType() == TextRangeType.VCS_CHANGED_TEXT
    );
  }

  private void initScopeFilter() {
    myUseScopeFilteringCb.setSelected(false);
    myScopeCombo.setEnabled(false);
    myUseScopeFilteringCb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myScopeCombo.setEnabled(myUseScopeFilteringCb.isSelected());
      }
    });
  }

  private void initFileTypeFilter() {
    FindDialog.initFileFilter(myFileFilter, myEnableFileNameFilterCb);
    myEnableFileNameFilterCb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateMaskWarning();
      }
    });
    myFileFilter.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        updateMaskWarning();
      }
    });
  }

  private void updateMaskWarning() {
    if (myEnableFileNameFilterCb.isSelected()) {
      String mask = (String)myFileFilter.getEditor().getItem();
      if (mask == null || !isMaskValid(mask)) {
        showWarningAndDisableOK();
        return;
      }
    }

    if (myMaskWarningLabel.isVisible()) {
      clearWarningAndEnableOK();
    }
  }

  private void showWarningAndDisableOK() {
    myMaskWarningLabel.setVisible(true);
    setOKActionEnabled(false);
  }

  private void clearWarningAndEnableOK() {
    myMaskWarningLabel.setVisible(false);
    setOKActionEnabled(true);
  }

  private static boolean isMaskValid(@Nonnull String mask) {
    try {
      FindInProjectUtil.createFileMaskCondition(mask);
    }
    catch (PatternSyntaxException e) {
      return false;
    }

    return true;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  public boolean isRearrangeCode() {
    return myCbRearrangeEntries.isSelected();
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(HELP_ID);
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();
    myLastRunOptions.saveOptimizeImportsState(isOptimizeImports());
    myLastRunOptions.saveRearrangeCodeState(isRearrangeCode());
    if (myEnableOnlyVCSChangedTextCb) {
      myLastRunOptions.saveProcessVcsChangedTextState(getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT);
    }
  }

  @Override
  public boolean isOptimizeImports() {
    return myCbOptimizeImports.isSelected();
  }

  @Override
  @Nullable
  public String getFileTypeMask() {
    if (myEnableFileNameFilterCb.isSelected()) {
      return (String)myFileFilter.getSelectedItem();
    }

    return null;
  }

  protected void createUIComponents() {
    myScopeCombo = new ScopeChooserCombo(myProject, false, false, FindSettings.getInstance().getDefaultScopeName());
    Disposer.register(myDisposable, myScopeCombo);
  }

  @Nullable
  @Override
  public SearchScope getSearchScope() {
    if (myUseScopeFilteringCb.isSelected()) {
      return myScopeCombo.getSelectedScope();
    }

    return null;
  }

  protected boolean shouldShowIncludeSubdirsCb() {
    return false;
  }

  @Override
  public TextRangeType getTextRangeType() {
    return myCbOnlyVcsChangedRegions.isEnabled() && myCbOnlyVcsChangedRegions.isSelected()
           ? TextRangeType.VCS_CHANGED_TEXT
           : TextRangeType.WHOLE_FILE;
  }
}
