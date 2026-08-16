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
package consulo.ui.ex.awt;

import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * TODO - check xawt.mwm_decor_title for linux
 *
 * @author VISTALL
 * @since 2024-11-26
 */
public interface AWTTitlelessDecorator extends TitlelessDecorator {
    class WindowsFameDecorator implements AWTTitlelessDecorator {
        private final JRootPane myRootPane;

        public WindowsFameDecorator(JRootPane rootPane) {
            myRootPane = rootPane;
        }

        @Override
        public void install(Window window) {
            myRootPane.putClientProperty("FlatLaf.fullWindowContent", true);
        }

        @Override
        public void makeLeftComponentLower(JComponent component) {
        }

        @Override
        public JComponent modifyRightComponent(JComponent rootPanel, JComponent rightComponent) {
            JPanel panel = new JPanel(new BorderLayout());

            JPanel placeholder = new JPanel();

            placeholder.putClientProperty("FlatLaf.fullWindowContent.buttonsPlaceholder", "win");

            rootPanel.putClientProperty("JComponent.titleBarCaption", (Function<Point, Boolean>) pt -> {
                return contains(placeholder, pt.x, pt.y);
            });

            panel.add(placeholder, BorderLayout.NORTH);
            panel.add(rightComponent, BorderLayout.CENTER);
            return panel;
        }

        private boolean contains(Component c, int x, int y) {
            return x >= 0 && y >= 0 && x < c.getWidth() && y < c.getHeight();
        }

        @Override
        public int getExtraTopLeftPadding(boolean fullScreen) {
            return 0;
        }

        @Override
        public int getExtraTopTopPadding() {
            return 0;
        }
    }

    class MacFrameDecorator implements AWTTitlelessDecorator {
        private final JRootPane myRootPane;

        public MacFrameDecorator(JRootPane rootPane) {
            myRootPane = rootPane;
        }

        @Override
        public void install(Window window) {
            myRootPane.putClientProperty("apple.awt.fullWindowContent", true);
            myRootPane.putClientProperty("apple.awt.transparentTitleBar", true);
            myRootPane.putClientProperty("apple.awt.windowTitleVisible", false);
            myRootPane.putClientProperty("FlatLaf.macOS.windowButtonsSpacing", "medium");
        }

        @Override
        public void makeLeftComponentLower(JComponent component) {
            component.setBorder(JBUI.Borders.empty(34, 0, 0, 0));
        }

        @Override
        public int getExtraTopLeftPadding(boolean fullScreen) {
            if (fullScreen) {
                return 0;
            }

            return 70;
        }

        @Override
        public int getExtraTopTopPadding() {
            return 4;
        }
    }

    void install(Window window);

    void makeLeftComponentLower(JComponent component);

    default JComponent modifyRightComponent(JComponent parent, JComponent rightComponent) {
        return rightComponent;
    }

    @Override
    default void makeLeftComponentLower(consulo.ui.Component component) {
        makeLeftComponentLower((JComponent) TargetAWT.to(component));
    }

    @Override
    default consulo.ui.Component modifyRightComponent(consulo.ui.Component parent, consulo.ui.Component rightComponent) {
        JComponent right = (JComponent) TargetAWT.to(rightComponent);

        JComponent modified = modifyRightComponent((JComponent) TargetAWT.to(parent), right);

        return modified == right ? rightComponent : TargetAWT.wrap(modified);
    }
}
