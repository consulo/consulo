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

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.ScrollableLayout;
import io.qt.widgets.QFrame;
import io.qt.widgets.QLayout;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtScrollableLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements ScrollableLayout {
    public DesktopQtScrollableLayoutImpl(Component component) {
        addImpl(component, null);
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QScrollArea scrollArea = new QScrollArea();
        scrollArea.setWidgetResizable(true);
        scrollArea.setFrameShape(QFrame.Shape.NoFrame);
        return scrollArea;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        ((QScrollArea) myComponent).setWidget(child.toQtComponent());
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        if (myComponent instanceof QScrollArea scrollArea && scrollArea.widget() == child.toQtComponent()) {
            scrollArea.takeWidget();
        }
    }
}
