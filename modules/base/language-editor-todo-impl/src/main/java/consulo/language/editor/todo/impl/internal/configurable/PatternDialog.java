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
package consulo.language.editor.todo.impl.internal.configurable;

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontDescription;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontDescriptionPanel;
import consulo.ide.impl.idea.application.options.colors.TextAttributesDescription;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.todo.TodoAttributesUtil;
import consulo.language.psi.search.TodoAttributes;
import consulo.language.psi.search.TodoPattern;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.image.Image;

import javax.swing.*;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
class PatternDialog extends DialogWrapper {
    private final TodoPattern myPattern;

    private final JComboBox<Image> myIconComboBox;
    private final JCheckBox myCaseSensitiveCheckBox;
    private final JTextField myPatternStringField;
    private final ColorAndFontDescriptionPanel myColorAndFontDescriptionPanel;
    private final ColorAndFontDescription myColorAndFontDescription;
    private final JCheckBox myUsedDefaultColorsCeckBox;

    @RequiredUIAccess
    public PatternDialog(Component parent, TodoPattern pattern) {
        super(parent, true);

        TodoAttributes attrs = pattern.getAttributes();
        myPattern = pattern;
        myIconComboBox = new JComboBox<>(new Image[]{
            PlatformIconGroup.generalTododefault(),
            PlatformIconGroup.generalTodoquestion(),
            PlatformIconGroup.generalTodoimportant()
        });
        myIconComboBox.setSelectedItem(attrs.getIcon());
        myIconComboBox.setRenderer(new TodoTypeListCellRenderer());
        myCaseSensitiveCheckBox = new JCheckBox(IdeLocalize.checkboxCaseSensitive().get(), pattern.isCaseSensitive());
        myPatternStringField = new JTextField(pattern.getPatternString());

        // use default colors check box
        myUsedDefaultColorsCeckBox = new JCheckBox(IdeLocalize.checkboxTodoUseDefaultColors().get());
        myUsedDefaultColorsCeckBox.setSelected(!attrs.shouldUseCustomTodoColor());

        myColorAndFontDescriptionPanel = new ColorAndFontDescriptionPanel();

        TextAttributes attributes = TodoAttributesUtil.getTextAttributes(myPattern.getAttributes());

        myColorAndFontDescription =
            new TextAttributesDescription(null, null, attributes, null, EditorColorsManager.getInstance().getGlobalScheme(), null, null) {
                @Override
                public void apply(EditorColorsScheme scheme) {

                }

                @Override
                public boolean isErrorStripeEnabled() {
                    return true;
                }
            };

        myColorAndFontDescriptionPanel.reset(myColorAndFontDescription);

        updateCustomColorsPanel();
        myUsedDefaultColorsCeckBox.addActionListener(e -> updateCustomColorsPanel());

        init();
    }

    @RequiredUIAccess
    private void updateCustomColorsPanel() {
        boolean useCustomColors = useCustomTodoColor();

        if (useCustomColors) {
            // restore controls
            myColorAndFontDescriptionPanel.reset(myColorAndFontDescription);
        }
        else {
            // disable controls
            myColorAndFontDescriptionPanel.resetDefault();
        }
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myPatternStringField;
    }

    @Override
    protected void doOKAction() {
        myPattern.setPatternString(myPatternStringField.getText().trim());
        myPattern.setCaseSensitive(myCaseSensitiveCheckBox.isSelected());

        TodoAttributes attrs = myPattern.getAttributes();
        attrs.setIcon((Image) myIconComboBox.getSelectedItem());
        attrs.setUseCustomTodoColor(useCustomTodoColor(), TodoAttributesUtil.getDefaultColorSchemeTextAttributes());

        if (useCustomTodoColor()) {
            myColorAndFontDescriptionPanel.apply(myColorAndFontDescription, null);
        }
        super.doOKAction();
    }


    private boolean useCustomTodoColor() {
        return !myUsedDefaultColorsCeckBox.isSelected();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gb = new GridBagConstraints(
            0,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 5, 10),
            0,
            0
        );

        JLabel patternLabel = new JLabel(IdeLocalize.labelTodoPattern().get());
        panel.add(patternLabel, gb);
        Dimension oldPreferredSize = myPatternStringField.getPreferredSize();
        myPatternStringField.setPreferredSize(new Dimension(300, oldPreferredSize.height));
        gb.gridx = 1;
        gb.gridwidth = GridBagConstraints.REMAINDER;
        gb.weightx = 1;
        panel.add(myPatternStringField, gb);

        JLabel iconLabel = new JLabel(IdeLocalize.labelTodoIcon().get());
        gb.gridy++;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0;
        panel.add(iconLabel, gb);

        gb.gridx = 1;
        gb.fill = GridBagConstraints.NONE;
        gb.gridwidth = GridBagConstraints.REMAINDER;
        gb.weightx = 0;
        panel.add(myIconComboBox, gb);

        gb.gridy++;
        gb.gridx = 0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.gridwidth = GridBagConstraints.REMAINDER;
        gb.weightx = 1;
        panel.add(myCaseSensitiveCheckBox, gb);

        gb.gridy++;
        gb.gridx = 0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.gridwidth = GridBagConstraints.REMAINDER;
        gb.weightx = 1;
        panel.add(myUsedDefaultColorsCeckBox, gb);

        gb.gridy++;
        gb.gridx = 0;
        gb.gridwidth = GridBagConstraints.REMAINDER;
        gb.weightx = 1;
        panel.add(myColorAndFontDescriptionPanel.getPanel(), gb);
        return panel;
    }
}
