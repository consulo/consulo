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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.AllIcons;
import consulo.application.HelpManager;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;
import consulo.find.FindSettings;
import consulo.find.ui.ScopeChooserCombo;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

/**
 * @author max
 */
public class LayoutProjectCodeDialog extends DialogWrapper implements ReformatFilesOptions {
    private static final String HELP_ID = "Reformat Code on Directory Dialog";

    private final Project myProject;
    private final String myText;
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
                                   boolean enableOnlyVCSChangedTextCb) {
        super(project, false);
        myText = text;
        myProject = project;
        myEnableOnlyVCSChangedTextCb = enableOnlyVCSChangedTextCb;
        myLastRunOptions = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

        $$$setupUI$$$();

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
        FindInProjectUtil.initFileFilter(myFileFilter, myEnableFileNameFilterCb);
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
            String mask = (String) myFileFilter.getEditor().getItem();
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
            return (String) myFileFilter.getSelectedItem();
        }

        return null;
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

    private void $$$setupUI$$$() {
        myScopeCombo = new ScopeChooserCombo(myProject, false, false, FindSettings.getInstance().getDefaultScopeName());
        Disposer.register(myDisposable, myScopeCombo);

        myWholePanel = new JPanel();
        myWholePanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        myWholePanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myOptionsPanel = new JPanel();
        myOptionsPanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(myOptionsPanel, BorderLayout.CENTER);
        myCbOptimizeImports = new JCheckBox();
        myCbOptimizeImports.setText("Optimize imports");
        myCbOptimizeImports.setMnemonic('O');
        myCbOptimizeImports.setDisplayedMnemonicIndex(0);
        myOptionsPanel.add(myCbOptimizeImports, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myCbRearrangeEntries = new JCheckBox();
        myCbRearrangeEntries.setText("Rearrange entries");
        myCbRearrangeEntries.setMnemonic('R');
        myCbRearrangeEntries.setDisplayedMnemonicIndex(0);
        myOptionsPanel.add(myCbRearrangeEntries, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myCbOnlyVcsChangedRegions = new JCheckBox();
        myCbOnlyVcsChangedRegions.setText("Only VCS changed text");
        myCbOnlyVcsChangedRegions.setMnemonic('V');
        myCbOnlyVcsChangedRegions.setDisplayedMnemonicIndex(5);
        myOptionsPanel.add(myCbOnlyVcsChangedRegions, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        myOptionsPanel.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myIncludeSubdirsCb = new JCheckBox();
        myIncludeSubdirsCb.setText("Include subdirectories");
        myIncludeSubdirsCb.setMnemonic('I');
        myIncludeSubdirsCb.setDisplayedMnemonicIndex(0);
        myOptionsPanel.add(myIncludeSubdirsCb, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myWholePanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myFiltersPanel = new JPanel();
        myFiltersPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        myWholePanel.add(myFiltersPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myFiltersPanel.add(panel3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myEnableFileNameFilterCb = new JBCheckBox();
        myEnableFileNameFilterCb.setText("File mask(s)");
        myEnableFileNameFilterCb.setMnemonic('F');
        myEnableFileNameFilterCb.setDisplayedMnemonicIndex(0);
        panel3.add(myEnableFileNameFilterCb, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myFileFilter = new ComboBox();
        panel3.add(myFileFilter, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myFiltersPanel.add(panel4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myUseScopeFilteringCb = new JCheckBox();
        myUseScopeFilteringCb.setText("Scope");
        myUseScopeFilteringCb.setMnemonic('S');
        myUseScopeFilteringCb.setDisplayedMnemonicIndex(0);
        panel4.add(myUseScopeFilteringCb, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel4.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        panel4.add(myScopeCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myMaskWarningLabel = new JLabel();
        myMaskWarningLabel.setText("File mask is invalid");
        myFiltersPanel.add(myMaskWarningLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 3, false));
        final Spacer spacer4 = new Spacer();
        myFiltersPanel.add(spacer4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myTitle = new JLabel();
        myTitle.setText("");
        myWholePanel.add(myTitle, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myWholePanel.add(panel5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 5), null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myWholePanel;
    }
}
