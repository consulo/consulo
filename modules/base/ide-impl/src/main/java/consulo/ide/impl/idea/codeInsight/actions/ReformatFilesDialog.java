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
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.FormatChangedTextUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.VCS_CHANGED_TEXT;
import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.WHOLE_FILE;

public class ReformatFilesDialog extends DialogWrapper implements ReformatFilesOptions {
    private JPanel myPanel;
    private CheckBox myOptimizeImports;
    private CheckBox myOnlyChangedText;
    private CheckBox myRearrangeEntriesCb;

    private final LastRunReformatCodeOptionsProvider myLastRunSettings;

    @RequiredUIAccess
    public ReformatFilesDialog(Project project, VirtualFile[] files) {
        super(project, true);

        $$$setupUI$$$();

        myLastRunSettings = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

        boolean canTargetVcsChanges = FormatChangedTextUtil.hasChanges(files, project);
        myOnlyChangedText.setEnabled(canTargetVcsChanges);
        myOnlyChangedText.setValue(canTargetVcsChanges && myLastRunSettings.getLastTextRangeType() == VCS_CHANGED_TEXT);
        myOptimizeImports.setValue(myLastRunSettings.getLastOptimizeImports());
        myRearrangeEntriesCb.setValue(myLastRunSettings.getLastRearrangeCode());

        setTitle(CodeInsightLocalize.dialogReformatFilesTitle());
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    public boolean isOptimizeImports() {
        return myOptimizeImports.getValue();
    }

    @Override
    public TextRangeType getTextRangeType() {
        return myOnlyChangedText.isEnabled() && myOnlyChangedText.getValue()
            ? VCS_CHANGED_TEXT
            : WHOLE_FILE;
    }

    @Override
    public boolean isRearrangeCode() {
        return myRearrangeEntriesCb.getValue();
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

    @Override
    public @Nullable SearchScope getSearchScope() {
        return null;
    }

    @Override
    public @Nullable String getFileTypeMask() {
        return null;
    }

    @RequiredUIAccess
    private void $$$setupUI$$$() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(1, 2, JBUI.insetsLeft(4), -1, -1));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, JBUI.emptyInsets(), -1, -1));
        myPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Label label1 = Label.create(CodeInsightLocalize.dialogReformatFilesReformatSelectedFilesLabel());
        panel1.add(TargetAWT.to(label1), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myOptimizeImports = CheckBox.create(CodeInsightLocalize.dialogReformatFilesOptimizeImportsCheckbox());
        panel1.add(TargetAWT.to(myOptimizeImports), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myOnlyChangedText = CheckBox.create(CodeInsightLocalize.reformatOptionVcsChangedRegion());
        panel1.add(TargetAWT.to(myOnlyChangedText), new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myRearrangeEntriesCb = CheckBox.create(CodeInsightLocalize.reformatOptionRearrangeEntries());
        panel1.add(TargetAWT.to(myRearrangeEntriesCb), new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Spacer spacer2 = new Spacer();
        myPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }
}
