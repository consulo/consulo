/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.plaf2.flat;

import com.formdev.flatlaf.ui.FlatTextFieldUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import consulo.codeEditor.Editor;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.internal.BasicEditorTextFieldUI;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.Wrapper;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.event.FocusListener;

/**
 * @author VISTALL
 * @see FlatTextFieldUI
 * @since 2024-11-30
 */
public class FlatEditorTextFieldUI extends BasicEditorTextFieldUI {
    public static BasicEditorTextFieldUI createUI(JComponent c) {
        return new FlatEditorTextFieldUI();
    }

    private FocusListener myFocusListener;

    private Insets myTextEditorMargin;

    @Override
    public void installUI(JComponent c) {
        myTextEditorMargin = UIManager.getInsets("TextField.margin");
        if (myTextEditorMargin == null) {
            myTextEditorMargin = JBUI.insets(2);
        }

        super.installUI(c);

        // necessary to update focus border and background
        myFocusListener = new FlatUIUtils.RepaintFocusListener(c, null);
        c.addFocusListener(myFocusListener);

        if (c instanceof EditorTextField field) {
            editorChanged(field);
        }
    }

    @Override
    public void editorChanged(EditorTextField field) {
        Wrapper wrapper = field.getEditorWrapper();
        if (wrapper == null) {
            return;
        }

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(myTextEditorMargin).asUIResource());
    }

    @Override
    public void suffixChanged(EditorTextField field, JComponent component) {
        InplaceComponent.prepareLeadingOrTrailingComponent(component);

        component.setOpaque(false);
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);

        c.removeFocusListener(myFocusListener);

        myTextEditorMargin = null;

        if (c instanceof EditorTextField field) {
            LookAndFeel.uninstallBorder(field.getEditorWrapper());
        }
    }

    @Override
    protected void paintBackground(Graphics g, EditorTextField c) {
        if (FlatUIUtils.isCellEditor(c)) {
            return;
        }
        
        float arc = FlatUIUtils.getBorderArc(c);

        if (arc > 0) {
            FlatUIUtils.paintParentBackground(g, c);
        }
        float focusWidth = FlatUIUtils.getBorderFocusWidth(c);

        // paint background
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);

            g2.setColor(c.getBackground());
            FlatUIUtils.paintComponentBackground(g2, 0, 0, c.getWidth(), c.getHeight(), focusWidth, arc);
        }
        finally {
            g2.dispose();
        }
    }

    @Override
    protected void setBorder(JComponent c) {
        Border border = c.getBorder();
        if (border == null || border instanceof UIResource) {
            Border textBorder = UIManager.getBorder("TextField.border");
            c.setBorder(textBorder);
        }
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        EditorTextField editorTextField = (EditorTextField) c;

        Editor editor = editorTextField.getEditor();

        Dimension size = JBUI.size(1, 10);
        if (editor != null) {
            size.height = editor.getLineHeight();

            size.height = Math.max(size.height, JBUIScale.scale(16));

            JBInsets.addTo(size, editorTextField.getInsets());
            JBInsets.addTo(size, editor.getInsets());
        }

        JBInsets.addTo(size, myTextEditorMargin);

        return size;
    }
}
