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
package consulo.ui.ex;

import consulo.ui.Component;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public interface TitlelessDecorator {
    String MAIN_WINDOW = "MainWindow";
    String WELCOME_WINDOW = "WelcomeWindow";

    TitlelessDecorator NOTHING = new TitlelessDecorator() {
        @Override
        public void makeLeftComponentLower(Component component) {
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

    void makeLeftComponentLower(Component component);

    int getExtraTopLeftPadding(boolean fullScreen);

    int getExtraTopTopPadding();

    default Component modifyRightComponent(Component parent, Component rightComponent) {
        return rightComponent;
    }
}
