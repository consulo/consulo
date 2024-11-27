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
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * TODO - check xawt.mwm_decor_title for minux
 *
 * @author VISTALL
 * @since 2024-11-26
 */
public interface TitlelessDecorator {
    static TitlelessDecorator of(@Nonnull JRootPane pane) {
        if (Platform.current().os().isMac()) {
            return new MacFrameDecorator(pane);
        }

        return NOTHING;
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
        }

        @Override
        public void makeLeftComponentLower(JComponent component) {
            component.setBorder(JBUI.Borders.empty(26, 0, 0, 0));
        }

        @Override
        public int getExtraTopLeftPadding() {
            return 60;
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
        public int getExtraTopLeftPadding() {
            return 0;
        }
    };

    void install(Window window);

    void makeLeftComponentLower(JComponent component);

    int getExtraTopLeftPadding();
}
