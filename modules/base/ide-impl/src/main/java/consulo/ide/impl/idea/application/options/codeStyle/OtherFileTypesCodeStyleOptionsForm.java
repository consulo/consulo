/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.codeStyle;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.configurable.ConfigurationException;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.plain.PlainTextFileType;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Used for non-language settings (if file type is not supported by Intellij IDEA), for example, plain text.
 *
 * @author Rustam Vishnyakov.
 */
public class OtherFileTypesCodeStyleOptionsForm extends CodeStyleAbstractPanel {
    private final IndentOptionsEditorWithSmartTabs myIndentOptionsEditor;
    private JPanel myIndentOptionsPanel;
    private JPanel myTopPanel;

    @RequiredUIAccess
    protected OtherFileTypesCodeStyleOptionsForm(@Nonnull CodeStyleSettings settings) {
        super(settings);
        init();
        myIndentOptionsEditor = new IndentOptionsEditorWithSmartTabs();
        myIndentOptionsPanel.add(myIndentOptionsEditor.createPanel(), BorderLayout.CENTER);
        addPanelToWatch(myIndentOptionsPanel);
    }

    @Override
    protected int getRightMargin() {
        return 0;
    }

    @Nullable
    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
        return null;
    }

    @Nonnull
    @Override
    protected FileType getFileType() {
        return PlainTextFileType.INSTANCE;
    }

    @Nullable
    @Override
    protected String getPreviewText() {
        return null;
    }

    @Override
    public void apply(CodeStyleSettings settings) throws ConfigurationException {
        myIndentOptionsEditor.apply(settings, settings.OTHER_INDENT_OPTIONS);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        return myIndentOptionsEditor.isModified(settings, settings.OTHER_INDENT_OPTIONS);
    }

    @Nullable
    @Override
    public JComponent getPanel() {
        return myTopPanel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        myIndentOptionsEditor.reset(settings, settings.OTHER_INDENT_OPTIONS);
    }

    private void init() {
        myTopPanel = new JPanel();
        myTopPanel.setLayout(new GridLayoutManager(2, 2, JBUI.insetsLeft(20), -1, -1));
        myIndentOptionsPanel = new JPanel();
        myIndentOptionsPanel.setLayout(new BorderLayout(0, 0));
        myIndentOptionsPanel.putClientProperty("BorderFactoryClass", "");
        myTopPanel.add(
            myIndentOptionsPanel,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myTopPanel.add(
            new Spacer(),
            new GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        myTopPanel.add(
            TargetAWT.to(Label.create(CodeStyleLocalize.codeStyleOtherLabel())),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
    }
}