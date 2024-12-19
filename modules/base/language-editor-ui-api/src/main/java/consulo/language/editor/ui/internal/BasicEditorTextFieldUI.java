/*
 * Copyright 2013-2019 consulo.io
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
package consulo.language.editor.ui.internal;

import consulo.codeEditor.Editor;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.awt.JBInsets;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;

public class BasicEditorTextFieldUI extends BasicPanelUI {
    public static BasicEditorTextFieldUI createUI(JComponent c) {
        return new BasicEditorTextFieldUI();
    }

    public void editorChanged(EditorTextField field) {
    }

    public void suffixChanged(EditorTextField field, JComponent component) {
    }

    @Override
    public void installUI(JComponent c) {
        setBorder(c);
    }

    protected void setBorder(JComponent c) {
        Border border = c.getBorder();
        if (border == null || border instanceof UIResource) {
            c.setBorder(UIManager.getBorder("TextField.border"));
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        LookAndFeel.uninstallBorder(c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        EditorTextField field = (EditorTextField) c;

        paintBackground(g, field);
    }

    protected void paintBackground(Graphics g, EditorTextField field) {
        g.setColor(field.getBackground());
        g.fillRect(0, 0, field.getWidth(), field.getHeight());
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        EditorTextField editorTextField = (EditorTextField) c;
        Editor editor = editorTextField.getEditor();
        Dimension size = new Dimension(1, 20);
        if (editor != null) {
            size.height = editor.getLineHeight();

            JBInsets.addTo(size, editorTextField.getInsets());
            JBInsets.addTo(size, editor.getInsets());
        }

        return size;
    }
}