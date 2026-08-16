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

import consulo.desktop.qt.ui.impl.DesktopQtTextItemPresentation;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import io.qt.widgets.QTabWidget;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTabImpl implements Tab {
    private BiConsumer<Tab, TextItemPresentation> myRenderer = (tab, presentation) -> presentation.append(toString());

    private @Nullable QtComponentDelegate<?> myComponent;

    private @Nullable QTabWidget myTabWidget;

    private @Nullable QWidget myContent;

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
    }

    @Override
    public void select() {
        int index = getIndex();
        if (myTabWidget != null && index != -1) {
            myTabWidget.setCurrentIndex(index);
        }
    }

    @Override
    public void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public void update() {
        int index = getIndex();
        if (myTabWidget == null || index == -1) {
            return;
        }

        DesktopQtTextItemPresentation item = new DesktopQtTextItemPresentation();
        myRenderer.accept(this, item);

        myTabWidget.setTabText(index, item.toString());
    }

    public void setComponent(Component component) {
        myComponent = (QtComponentDelegate<?>) component;
    }

    public @Nullable QtComponentDelegate<?> getComponent() {
        return myComponent;
    }

    /**
     * A tab has no identity of its own inside a {@link QTabWidget} - only a position, which shifts whenever
     * another tab is dropped - so the position is asked of the widget every time instead of being remembered.
     */
    public int getIndex() {
        return myTabWidget == null || myContent == null ? -1 : myTabWidget.indexOf(myContent);
    }

    public void initialize(QTabWidget tabWidget) {
        QWidget content = null;

        if (myComponent != null) {
            myComponent.bind(tabWidget, null);

            content = myComponent.toQtComponent();
        }

        if (content == null) {
            content = new QWidget();
        }

        myTabWidget = tabWidget;
        myContent = content;

        tabWidget.addTab(content, "");

        update();
    }

    public void detach() {
        int index = getIndex();
        if (myTabWidget != null && index != -1) {
            myTabWidget.removeTab(index);
        }

        myTabWidget = null;
        myContent = null;

        if (myComponent != null) {
            myComponent.setParent(null);
        }
    }
}
