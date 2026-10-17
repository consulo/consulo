/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.plaf2.flat.FlatOptionButtonUI;
import consulo.localize.LocalizeValue;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
class DesktopOptionButtonImpl extends DesktopButtonImpl implements OptionButton {
    private static final String SHOW_OPTIONS_ACTION = "consulo.showOptions";

    class MyOptionButton extends MyButton {
        MyOptionButton(LocalizeValue text) {
            super(text);

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                SHOW_OPTIONS_ACTION);
            getActionMap().put(SHOW_OPTIONS_ACTION, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showOptions();
                }
            });
        }

        @Override
        public String getUIClassID() {
            return UIManager.get(FlatOptionButtonUI.UI_CLASS_ID) != null ? FlatOptionButtonUI.UI_CLASS_ID : super.getUIClassID();
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            int id = e.getID();
            boolean press = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_RELEASED || id == MouseEvent.MOUSE_CLICKED;
            if (press && isEnabled() && FlatOptionButtonUI.isOverArrow(this, e.getX())) {
                if (id == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)) {
                    requestFocusInWindow();

                    showOptions();
                }

                e.consume();
                return;
            }

            super.processMouseEvent(e);
        }
    }

    private List<OptionButtonItem> myOptions = List.of();

    DesktopOptionButtonImpl(LocalizeValue text) {
        super(text);
    }

    @Override
    protected MyButton createButton(LocalizeValue text) {
        return new MyOptionButton(text);
    }

    @Override
    public List<OptionButtonItem> getOptions() {
        return myOptions;
    }

    @RequiredUIAccess
    @Override
    public void setOptions(List<OptionButtonItem> options) {
        myOptions = List.copyOf(options);

        MyButton button = toAWTComponent();
        button.putClientProperty(FlatOptionButtonUI.HAS_OPTIONS, myOptions.isEmpty() ? null : Boolean.TRUE);
        button.revalidate();
        button.repaint();
    }

    @RequiredUIAccess
    @Override
    public void showOptions() {
        MyButton button = toAWTComponent();
        if (myOptions.isEmpty() || !button.isShowing()) {
            return;
        }

        JPopupMenu popup = new JPopupMenu();
        for (OptionButtonItem option : myOptions) {
            JMenuItem item = new JMenuItem();
            updateTextForButton(item, option.text());
            item.setIcon(TargetAWT.to(option.icon()));
            item.setEnabled(option.enabled());
            item.addActionListener(e -> option.clickListener()
                .onEvent(new ClickEvent(DesktopOptionButtonImpl.this, DesktopAWTInputDetails.convert(button, e))));
            popup.add(item);
        }

        Dimension size = popup.getPreferredSize();
        if (size.width < button.getWidth()) {
            popup.setPopupSize(button.getWidth(), size.height);
        }

        popup.show(button, 0, button.getHeight());
    }
}
