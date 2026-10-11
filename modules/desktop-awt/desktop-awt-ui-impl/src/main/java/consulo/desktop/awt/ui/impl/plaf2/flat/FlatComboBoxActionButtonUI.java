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
package consulo.desktop.awt.ui.impl.plaf2.flat;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import consulo.desktop.awt.ui.impl.action.ComboBoxActionButtonUI;
import consulo.desktop.awt.ui.impl.action.CustomPopupComboBox;

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
public class FlatComboBoxActionButtonUI extends FlatComboBoxUI implements ComboBoxActionButtonUI {
    public static class IdeComboBoxPopup implements ComboPopup {
        private final JList<?> myDummyList = new JList<>();
        private final CustomPopupComboBox<?> myButton;

        IdeComboBoxPopup(CustomPopupComboBox<?> button) {
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
            return myButton.isCustomPopupVisible();
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
                    if (myButton.isEnabled()) {
                        show();
                    }
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
        return new FlatComboBoxActionButtonUI();
    }

    @Override
    public void setPopupVisible(JComboBox c, boolean v) {
        CustomPopupComboBox<?> boxButton = (CustomPopupComboBox<?>) c;
        if (v) {
            boxButton.showPopupImpl();
        }
        else {
            boxButton.hidePopupImpl();
        }
    }

    @Override
    protected ComboPopup createPopup() {
        return new IdeComboBoxPopup((CustomPopupComboBox<?>) comboBox);
    }

    @Override
    public boolean isPopupVisible(JComboBox c) {
        return ((CustomPopupComboBox<?>) c).isCustomPopupVisible();
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

        updateArrowState(((CustomPopupComboBox<?>) c).isArrowVisible());
    }
}
