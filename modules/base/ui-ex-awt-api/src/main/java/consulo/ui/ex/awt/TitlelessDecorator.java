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

import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.project.ui.wm.IdeFrameState;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * TODO - check xawt.mwm_decor_title for linux
 *
 * @author VISTALL
 * @since 2024-11-26
 */
public interface TitlelessDecorator {
    String MAIN_WINDOW = "MainWindow";
    String WELCOME_WINDOW = "WelcomeWindow";

    @Nonnull
    static TitlelessDecorator of(@Nonnull JRootPane pane) {
        return of(pane, "");
    }

    @Nonnull
    static TitlelessDecorator of(@Nonnull JRootPane pane, @Nonnull String windowId) {
        PlatformOperatingSystem os = Platform.current().os();
        if (os.isMac()) {
            return new MacFrameDecorator(pane);
        }

        if (os.isWindows() || os.isLinux()) {
            // no sence for it - we already without title
            if (MAIN_WINDOW.equals(windowId)) {
                return NOTHING;
            }

            if (WELCOME_WINDOW.equals(windowId)) {
                return new WindowsFameDecorator(pane);
            }

            // FIXME for now we not support other titleless - due bug with moving
        }

        return NOTHING;
    }

    class WindowsFameDecorator implements TitlelessDecorator {
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

        @Nonnull
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

    class MacFrameDecorator implements TitlelessDecorator {
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

    TitlelessDecorator NOTHING = new TitlelessDecorator() {
        @Override
        public void install(Window window) {
        }

        @Override
        public void makeLeftComponentLower(JComponent component) {
        }

        @Override
        public int getExtraTopLeftPadding(boolean fullScreen) {
            return 0;
        }

        @Override
        public int getExtraTopTopPadding() {
            return 0;
        }
    };

    void install(Window window);

    void makeLeftComponentLower(JComponent component);

    @Nonnull
    default JComponent modifyRightComponent(JComponent parent, JComponent rightComponent) {
        return rightComponent;
    }

    int getExtraTopLeftPadding(boolean fullScreen);

    int getExtraTopTopPadding();
}
