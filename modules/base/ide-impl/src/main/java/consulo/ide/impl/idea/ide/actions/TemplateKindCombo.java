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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.ui.UISettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.util.lang.Trinity;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboboxSpeedSearch;
import consulo.ui.ex.awt.ComboboxWithBrowseButton;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class TemplateKindCombo extends ComboboxWithBrowseButton {
    public TemplateKindCombo() {
        getComboBox().setRenderer(new ColoredListCellRenderer() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if (value instanceof Trinity trinity) {
                    append((String)trinity.first);
                    setIcon((Image)trinity.second);
                }
            }
        });

        new ComboboxSpeedSearch(getComboBox()) {
            @Override
            protected String getElementText(Object element) {
                return element instanceof Trinity trinity ? (String)trinity.first : null;
            }
        };
        setButtonListener(null);
    }

    public void addItem(String presentableName, Image icon, String templateName) {
        getComboBox().addItem(new Trinity<>(presentableName, icon, templateName));
    }

    public String getSelectedName() {
        //noinspection unchecked
        Trinity<String, Image, String> trinity = (Trinity<String, Image, String>)getComboBox().getSelectedItem();
        if (trinity == null) {
            // design time
            return null;
        }
        return trinity.third;
    }

    public void setSelectedName(@Nullable String name) {
        if (name == null) {
            return;
        }
        ComboBoxModel model = getComboBox().getModel();
        for (int i = 0, n = model.getSize(); i < n; i++) {
            Trinity<String, Image, String> trinity = (Trinity<String, Image, String>)model.getElementAt(i);
            if (name.equals(trinity.third)) {
                getComboBox().setSelectedItem(trinity);
                return;
            }
        }
    }

    public void registerUpDownHint(JComponent component) {
        new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                if (e.getInputEvent() instanceof KeyEvent keyEvent) {
                    int code = keyEvent.getKeyCode();
                    scrollBy(code == KeyEvent.VK_DOWN ? 1 : code == KeyEvent.VK_UP ? -1 : 0);
                }
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyEvent.VK_UP, KeyEvent.VK_DOWN), component);
    }

    private void scrollBy(int delta) {
        if (delta == 0) {
            return;
        }
        int size = getComboBox().getModel().getSize();
        int next = getComboBox().getSelectedIndex() + delta;
        if (next < 0 || next >= size) {
            if (!UISettings.getInstance().CYCLE_SCROLLING) {
                return;
            }
            next = (next + size) % size;
        }
        getComboBox().setSelectedIndex(next);
    }

    /**
     * @param listener pass <code>null</code> to hide browse button
     */
    public void setButtonListener(@Nullable ActionListener listener) {
        getButton().setVisible(listener != null);
        if (listener != null) {
            addActionListener(listener);
        }
    }

    public void clear() {
        getComboBox().removeAllItems();
    }
}
