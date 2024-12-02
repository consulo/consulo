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

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import consulo.ui.ex.awt.action.ComboBoxButtonImpl;
import consulo.ui.ex.awt.action.ComboBoxButtonUI;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
public class FlatComboBoxButtonUI extends FlatComboBoxUI implements ComboBoxButtonUI {
    public static class IdeComboBoxPopup implements ComboPopup {
        private final JList<?> myDummyList = new JList<>();
        private final ComboBoxButtonImpl myButton;

        IdeComboBoxPopup(ComboBoxButtonImpl button) {
            myButton = button;
            // some ui register listeners to JList of popup
            // just return dummy instance
            // also override default UI since, some ui like Aqua can just skip list if is not aqua list ui
            myDummyList.setUI(new BasicListUI());
        }

        @Override
        public void show() {
            myButton.showPopupImpl();
        }

        @Override
        public void hide() {
            myButton.hidePopupImpl();
        }

        @Override
        public boolean isVisible() {
            return myButton.getCurrentPopupCanceler() != null;
        }

        @Override
        public JList getList() {
            return myDummyList;
        }

        @Override
        public MouseListener getMouseListener() {
            return new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    show();
                }
            };
        }

        @Override
        public MouseMotionListener getMouseMotionListener() {
            return null;
        }

        @Override
        public KeyListener getKeyListener() {
            return null;
        }

        @Override
        public void uninstallingUI() {
        }
    }

    public static ComponentUI createUI(JComponent c) {
        return new FlatComboBoxButtonUI();
    }

    @Override
    public Accessible getAccessibleChild(JComponent c, int i) {
        return null;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        float focusWidth = FlatUIUtils.getBorderFocusWidth(c);
        float arc = FlatUIUtils.getBorderArc(c);
        boolean paintBackground = c.isOpaque();

        // check whether used as cell renderer
        boolean isCellRenderer = c.getParent() instanceof CellRendererPane;
        if (isCellRenderer) {
            focusWidth = 0;
            arc = 0;
            paintBackground = isCellRendererBackgroundChanged();
        }

        // fill background if opaque to avoid garbage if user sets opaque to true
        if (c.isOpaque() && (focusWidth > 0 || arc > 0))
            FlatUIUtils.paintParentBackground(g, c);

        Graphics2D g2 = (Graphics2D) g;
        Object[] oldRenderingHints = FlatUIUtils.setRenderingHints(g2);

        int width = c.getWidth();
        int height = c.getHeight();
        int arrowX = arrowButton.getX();
        int arrowWidth = arrowButton.getWidth();
        boolean paintButton = (comboBox.isEditable() || "button".equals(buttonStyle)) &&
            !"none".equals(buttonStyle) &&
            !isMacStyle();
        boolean enabled = comboBox.isEnabled();
        boolean isLeftToRight = comboBox.getComponentOrientation().isLeftToRight();

        // paint background
        if (paintBackground) {
            g2.setColor(getBackground(enabled));
            FlatUIUtils.paintComponentBackground(g2, 0, 0, width, height, focusWidth, arc);

            // paint arrow button background
            if (enabled && !isCellRenderer && arrowButton != null && arrowButton.isVisible()) { // FIXME do not render arrow button if not visible
                Color buttonColor = paintButton
                    ? buttonEditableBackground
                    : (buttonFocusedBackground != null || focusedBackground != null) && isPermanentFocusOwner(comboBox)
                    ? (buttonFocusedBackground != null ? buttonFocusedBackground : focusedBackground)
                    : buttonBackground;
                if (buttonColor != null) {
                    g2.setColor(buttonColor);
                    if (isMacStyle()) {
                        Insets insets = comboBox.getInsets();
                        int gap = UIScale.scale(2);
                        FlatUIUtils.paintComponentBackground(g2, arrowX + gap, insets.top + gap,
                            arrowWidth - (gap * 2), height - insets.top - insets.bottom - (gap * 2),
                            0, arc - focusWidth);
                    }
                    else {
                        Shape oldClip = g2.getClip();
                        if (isLeftToRight)
                            g2.clipRect(arrowX, 0, width - arrowX, height);
                        else
                            g2.clipRect(0, 0, arrowX + arrowWidth, height);
                        FlatUIUtils.paintComponentBackground(g2, 0, 0, width, height, focusWidth, arc);
                        g2.setClip(oldClip);
                    }
                }
            }

            // paint vertical line between value and arrow button
            if (paintButton) {
                Color separatorColor = enabled ? buttonSeparatorColor : buttonDisabledSeparatorColor;
                if (separatorColor != null && buttonSeparatorWidth > 0) {
                    g2.setColor(separatorColor);
                    float lw = UIScale.scale(buttonSeparatorWidth);
                    float lx = isLeftToRight ? arrowX : arrowX + arrowWidth - lw;
                    g2.fill(new Rectangle2D.Float(lx, focusWidth, lw, height - 1 - (focusWidth * 2)));
                }
            }
        }

        // avoid that the "current value" renderer is invoked with enabled antialiasing
        FlatUIUtils.resetRenderingHints(g2, oldRenderingHints);

        paint(g, c);
    }

    private boolean isCellRendererBackgroundChanged() {
        // parent is a CellRendererPane, parentParent is e.g. a JTable
        Container parentParent = comboBox.getParent().getParent();
        return parentParent != null && !comboBox.getBackground().equals(parentParent.getBackground());
    }

    private boolean isMacStyle() {
        return "mac".equals(buttonStyle);
    }

    @Override
    public void setPopupVisible(JComboBox c, boolean v) {
        ComboBoxButtonImpl boxButton = (ComboBoxButtonImpl) c;
        if (v) {
            boxButton.showPopupImpl();
        }
        else {
            boxButton.hidePopupImpl();
        }
    }

    @Override
    protected ComboPopup createPopup() {
        return new IdeComboBoxPopup((ComboBoxButtonImpl) comboBox);
    }

    @Override
    public boolean isPopupVisible(JComboBox c) {
        ComboBoxButtonImpl boxButton = (ComboBoxButtonImpl) c;

        return boxButton.getCurrentPopupCanceler() != null;
    }

    @Override
    public void updateArrowState(boolean visible) {
        if (arrowButton != null) {
            arrowButton.setVisible(visible);
        }
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        ComboBoxButtonImpl boxButton = (ComboBoxButtonImpl) c;

        updateArrowState(boxButton.getOnClickListener() == null);
    }
}
