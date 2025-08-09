/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.refactoring.ui;

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;
import consulo.language.editor.ui.EditorSettingsProvider;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.table.JBListTable;
import consulo.ui.ex.awt.table.JBTableRowEditor;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-08-09
 */
public abstract class JBListTableWitEditors extends JBListTable {
    public JBListTableWitEditors(@Nonnull JTable t) {
        super(t);
    }

    public static JComponent createEditorTextFieldPresentation(Project project, FileType type, String text, boolean selected, boolean focused) {
        EditorTextField field = new EditorTextField(text, project, type);
        field.putClientProperty("JComboBox.isTableCellEditor", true);
        field.setBorder(JBUI.Borders.empty());

        Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
        font = new Font(font.getFontName(), font.getStyle(), 12);
        field.setFont(font);
        field.addSettingsProvider(EditorSettingsProvider.NO_WHITESPACE);

        if (selected && focused) {
            field.setAsRendererWithSelection(UIUtil.getTableSelectionBackground(), UIUtil.getTableSelectionForeground());
        }

        return field;
    }

    @Override
    protected void installPaddingAndBordersForEditors(JBTableRowEditor editor) {
        List<EditorTextField> editors = UIUtil.findComponentsOfType(editor, EditorTextField.class);
        for (EditorTextField textField : editors) {
            textField.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
            textField.putClientProperty("JBListTable.isTableCellEditor", Boolean.TRUE);
        }
    }
}
