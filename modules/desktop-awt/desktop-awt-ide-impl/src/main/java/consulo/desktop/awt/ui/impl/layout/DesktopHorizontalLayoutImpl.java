/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl.layout;

import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.layout.HorizontalLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopHorizontalLayoutImpl extends DesktopLayoutBase<JPanel, StaticPosition> implements HorizontalLayout {
    public DesktopHorizontalLayoutImpl(int gapInPixesl) {
        initDefaultPanel(new consulo.ui.ex.awt.HorizontalLayout(JBUI.scale(gapInPixesl)));
    }

    @Override
    protected Object convertConstraints(StaticPosition constraint) {
        switch (constraint) {
            case LEFT:
                return consulo.ui.ex.awt.HorizontalLayout.LEFT;
            case RIGHT:
                return consulo.ui.ex.awt.HorizontalLayout.RIGHT;
            case CENTER:
                return consulo.ui.ex.awt.HorizontalLayout.CENTER;
            default:
                throw new IllegalArgumentException(constraint.name());
        }
    }
}
