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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.content.scope.SearchScope;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.VCS_CHANGED_TEXT;
import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.WHOLE_FILE;

public class ReformatFilesDialog extends DialogWrapper implements ReformatFilesOptions {
    private JPanel myPanel;
    private JCheckBox myOptimizeImports;
    private JCheckBox myOnlyChangedText;
    private JCheckBox myRearrangeEntriesCb;

    private final LastRunReformatCodeOptionsProvider myLastRunSettings;

    public ReformatFilesDialog(@Nonnull Project project, @Nonnull VirtualFile[] files) {
        super(project, true);

        $$$setupUI$$$();

        myLastRunSettings = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

        boolean canTargetVcsChanges = FormatChangedTextUtil.hasChanges(files, project);
        myOnlyChangedText.setEnabled(canTargetVcsChanges);
        myOnlyChangedText.setSelected(canTargetVcsChanges && myLastRunSettings.getLastTextRangeType() == VCS_CHANGED_TEXT);
        myOptimizeImports.setSelected(myLastRunSettings.getLastOptimizeImports());
        myRearrangeEntriesCb.setSelected(myLastRunSettings.getLastRearrangeCode());

        setTitle(CodeInsightLocalize.dialogReformatFilesTitle());
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    public boolean isOptimizeImports() {
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

    private void $$$setupUI$$$() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(1, 2, JBUI.insetsLeft(4), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, JBUI.emptyInsets(), -1, -1));
        myPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, CodeInsightBundle.message("dialog.reformat.files.reformat.selected.files.label"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myOptimizeImports = new JCheckBox();
        this.$$$loadButtonText$$$(myOptimizeImports, CodeInsightBundle.message("dialog.reformat.files.optimize.imports.checkbox"));
        panel1.add(myOptimizeImports, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myOnlyChangedText = new JCheckBox();
        this.$$$loadButtonText$$$(myOnlyChangedText, CodeInsightBundle.message("reformat.option.vcs.changed.region"));
        panel1.add(myOnlyChangedText, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myRearrangeEntriesCb = new JCheckBox();
        myRearrangeEntriesCb.setText("Rearrange entries");
        myRearrangeEntriesCb.setMnemonic('R');
        myRearrangeEntriesCb.setDisplayedMnemonicIndex(0);
        panel1.add(myRearrangeEntriesCb, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        myPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }
}
