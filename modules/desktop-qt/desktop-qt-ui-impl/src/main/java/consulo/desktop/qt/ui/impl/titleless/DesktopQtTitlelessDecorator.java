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
package consulo.desktop.qt.ui.impl.titleless;

import consulo.ui.Component;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.TitlelessDecorator;

/**
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtTitlelessDecorator implements TitlelessDecorator {
    private final int myTopPadding;

    public DesktopQtTitlelessDecorator(int topPadding) {
        myTopPadding = topPadding;
    }

    @Override
    public void makeLeftComponentLower(Component component) {
        component.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, null, myTopPadding);
    }

    @Override
    public Component modifyRightComponent(Component parent, Component rightComponent) {
        rightComponent.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, null, myTopPadding);
        return rightComponent;
    }

    @Override
    public int getExtraTopLeftPadding(boolean fullScreen) {
        return 0;
    }

    @Override
    public int getExtraTopTopPadding() {
        return myTopPadding;
    }
}
