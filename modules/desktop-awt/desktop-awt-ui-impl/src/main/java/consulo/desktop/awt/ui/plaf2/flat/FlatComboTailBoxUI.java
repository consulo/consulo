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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * @author VISTALL
 * @since 2024-12-19
 */
public class FlatComboTailBoxUI extends FlatComboBoxUI {
    private static final String TAIL_PROPERTY = "JComboBox.trailingComponent";

    private final JPanel myTailPanel = new JPanel(new BorderLayout());

    private PropertyChangeListener myListener = evt -> {
        Object newValue = evt.getNewValue();
        if (newValue instanceof JComponent tail) {
            InplaceComponent.prepareLeadingOrTrailingComponent(tail);

            myTailPanel.add(tail);
            comboBox.repaint();
        }
        else {
            myTailPanel.removeAll();
        }
    };

    public static FlatComboTailBoxUI createUI(JComponent c) {
        return new FlatComboTailBoxUI();
    }

    public FlatComboTailBoxUI() {
        myTailPanel.setBorder(new EmptyBorder(0, 0, 0, 8));
        myTailPanel.setOpaque(false);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        Object tail = comboBox.getClientProperty(TAIL_PROPERTY);
        if (tail instanceof JComponent component) {
            InplaceComponent.prepareLeadingOrTrailingComponent(component);

            myTailPanel.add(component, BorderLayout.CENTER);
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        
        Object tail = c.getClientProperty(TAIL_PROPERTY);
        if (tail instanceof JComponent component) {
            myTailPanel.remove(component);
        }
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        comboBox.addPropertyChangeListener(TAIL_PROPERTY, myListener);
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        comboBox.removePropertyChangeListener(TAIL_PROPERTY, myListener);
    }

    @Override
    protected void installComponents() {
        super.installComponents();
        comboBox.add(myTailPanel);
    }

    @Override
    protected Rectangle rectangleForCurrentValue() {
        Rectangle rectangle = super.rectangleForCurrentValue();
        if (myTailPanel.getComponentCount() > 0) {
            rectangle.width -= myTailPanel.getWidth();

            if (!comboBox.getComponentOrientation().isLeftToRight()) {
                rectangle.x += arrowButton.getWidth() + myTailPanel.getWidth();
            }
        }

        return rectangle;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        Dimension size = super.getMinimumSize(c);
        if (myTailPanel.getComponentCount() > 0) {
            int height = myTailPanel.getPreferredSize().height;
            if (height > size.height) {
                size.height = height;
            }
        }
        return size;
    }

    @Override
    protected LayoutManager createLayoutManager() {
        return new ComboBoxLayoutManager() {
            final LayoutManager lm = FlatComboTailBoxUI.super.createLayoutManager();

            @Override
            public void layoutContainer(Container parent) {
                lm.layoutContainer(parent);

                if (myTailPanel.getComponentCount() == 0) {
                    return;
                }

                JComboBox cb = (JComboBox) parent;
                Dimension aps = arrowButton.getPreferredSize();

                Dimension pps = myTailPanel.getPreferredSize();
                int availableWidth = cb.getWidth() - aps.width;
                if (comboBox.getComponentOrientation().isLeftToRight()) {
                    myTailPanel.setBounds(
                        Math.max(availableWidth - pps.width, 0),
                        (cb.getHeight() - pps.height) / 2,
                        Math.min(pps.width, availableWidth),
                        pps.height
                    );
                }
                else {
                    myTailPanel.setBounds(
                        arrowButton.getWidth(),
                        (cb.getHeight() - pps.height) / 2,
                        Math.min(pps.width, availableWidth),
                        pps.height
                    );
                }
            }
        };
    }
}
