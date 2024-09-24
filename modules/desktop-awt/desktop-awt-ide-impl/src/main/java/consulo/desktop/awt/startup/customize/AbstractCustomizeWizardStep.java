/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.desktop.awt.startup.customize;

import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.UIUtil;

import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

public abstract class AbstractCustomizeWizardStep extends JPanel {
    protected static final int GAP = 20;

    protected abstract String getTitle();

    protected abstract String getHTMLHeader();

    protected abstract String getHTMLFooter();

    @Nonnull
    protected static Color getSelectionBackground() {
        return ColorUtil.mix(
            UIUtil.getListSelectionBackground(true),
            UIUtil.getLabelBackground(),
            StyleManager.get().getCurrentStyle().isDark() ? .5 : .75
        );
    }

    protected static JPanel createBigButtonPanel(LayoutManager layout, final JToggleButton anchorButton, final Runnable action) {
        return createBigButtonPanel(layout, anchorButton, false, action);
    }

    protected static JPanel createBigButtonPanel(
        LayoutManager layout,
        final JToggleButton anchorButton,
        boolean allowInverse,
        final Runnable action
    ) {
        final JPanel panel = new JPanel(layout) {
            @Override
            public Color getBackground() {
                if (anchorButton.isSelected()) {
                    return getSelectionBackground();
                }
                return super.getBackground();
            }
        };
        panel.setOpaque(anchorButton.isSelected());
        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
                if (allowInverse) {
                    anchorButton.setSelected(!anchorButton.isSelected());
                }
                else {
                    anchorButton.setSelected(true);
                }
                return true;
            }
        }.installOn(panel);
        anchorButton.addItemListener(new ItemListener() {
            boolean curState = anchorButton.isSelected();

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && curState != anchorButton.isSelected()) {
                    action.run();
                }
                curState = anchorButton.isSelected();
                panel.setOpaque(curState);
                panel.repaint();
            }
        });
        return panel;
    }

    Component getDefaultFocusedComponent() {
        return null;
    }

    public boolean beforeShown(boolean forward) {
        return false;
    }
}
