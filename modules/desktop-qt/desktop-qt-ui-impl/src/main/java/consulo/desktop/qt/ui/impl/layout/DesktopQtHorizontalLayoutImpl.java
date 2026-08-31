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
package consulo.desktop.qt.ui.impl.layout;

import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import io.qt.widgets.QBoxLayout;
import io.qt.widgets.QHBoxLayout;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtHorizontalLayoutImpl extends DesktopQtBoxLayoutComponent<StaticPosition> implements HorizontalLayout {
    private final int myGap;

    public DesktopQtHorizontalLayoutImpl(int gapInPixels) {
        myGap = gapInPixels;
    }

    @Override
    protected QBoxLayout createBoxLayout() {
        QHBoxLayout layout = new QHBoxLayout();
        layout.setSpacing(myGap);
        return layout;
    }

    @Override
    public HorizontalLayout add(Component component, StaticPosition constraint) {
        addImpl(component, convertConstraintsToLayoutData(constraint));
        return this;
    }

    @Override
    public void addStyle(HorizontalLayoutStyle style) {
    }
}
