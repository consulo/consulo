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
package consulo.desktop.qt.editor.impl.internal.bridge;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.layout.DesktopQtLayoutComponent;
import consulo.ui.Component;
import consulo.ui.layout.LayoutConstraint;
import io.qt.core.Qt;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QLayout;
import io.qt.widgets.QSplitter;
import io.qt.widgets.QSplitterHandle;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopQtEditorBridgeLayout extends DesktopQtLayoutComponent<LayoutConstraint, Object> {
    private static final int DIVIDER_WIDTH = 20;

    private final DesktopQtEditorBridge myBridge;

    private final QtComponentDelegate<?>[] myAttached = new QtComponentDelegate<?>[3];

    private int myExpectedCount;
    private boolean myEqualized;

    public DesktopQtEditorBridgeLayout(DesktopQtEditorBridge bridge) {
        myBridge = bridge;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QSplitter splitter = new QSplitter(Qt.Orientation.Horizontal) {
            @Override
            protected QSplitterHandle createHandle() {
                return new DesktopQtEditorBridgeDivider(orientation(), this, myBridge);
            }

            @Override
            protected void resizeEvent(QResizeEvent event) {
                super.resizeEvent(event);
                equalizeSizes();
            }
        };
        splitter.setHandleWidth(DIVIDER_WIDTH);
        splitter.setChildrenCollapsible(false);
        return splitter;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        int slot = layoutData instanceof Integer index ? index : 0;
        myAttached[slot] = child;

        int position = 0;
        for (int i = 0; i < slot; i++) {
            if (myAttached[i] != null) {
                position++;
            }
        }

        QSplitter splitter = (QSplitter) myComponent;
        splitter.insertWidget(position, child.toQtComponent());
        splitter.setStretchFactor(position, 1);

        myEqualized = false;
        equalizeSizes();
    }

    private void equalizeSizes() {
        if (myEqualized || !isAlive(myComponent)) {
            return;
        }

        QSplitter splitter = (QSplitter) myComponent;
        int count = splitter.count();
        int available = splitter.width() - splitter.handleWidth() * (count - 1);
        if (count < myExpectedCount || available <= 0) {
            return;
        }

        List<Integer> sizes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sizes.add(available / count);
        }
        splitter.setSizes(sizes);
        myEqualized = true;
    }

    @Override
    protected int compareLayoutData(@Nullable Object first, @Nullable Object second) {
        return Integer.compare(first instanceof Integer index ? index : 0, second instanceof Integer index ? index : 0);
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        for (int slot = 0; slot < myAttached.length; slot++) {
            if (myAttached[slot] == child) {
                myAttached[slot] = null;
            }
        }
    }

    public void repaintDivider() {
        if (!isAlive(myComponent)) {
            return;
        }
        QSplitter splitter = (QSplitter) myComponent;
        for (int index = 0; index < splitter.count(); index++) {
            QSplitterHandle handle = splitter.handle(index);
            if (handle != null) {
                handle.update();
            }
        }
    }

    void setComponents(List<Component> components) {
        myExpectedCount = components.size();
        for (int i = 0; i < components.size(); i++) {
            addImpl(components.get(i), i);
        }
    }
}
