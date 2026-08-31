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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.SwipeLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QStackedWidget;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtSwipeLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements SwipeLayout {
    private static class LayoutInfo {
        private final Supplier<Layout> myLayoutSupplier;

        private @Nullable Layout myLayout;

        LayoutInfo(Supplier<Layout> layoutSupplier) {
            myLayoutSupplier = layoutSupplier;
        }

        Layout get() {
            Layout layout = myLayout;
            if (layout == null) {
                myLayout = layout = myLayoutSupplier.get();
            }
            return layout;
        }
    }

    private final Map<String, LayoutInfo> myLayoutInfos = new LinkedHashMap<>();

    @Override
    protected QWidget createQt(QWidget parent) {
        return new QStackedWidget(parent);
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    /**
     * A page is only built when it is first swiped to, so a stack bound before any swipe would hold nothing
     * at all - the carousel the web frontend uses shows its first slide on its own, and this matches that.
     */
    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        if (((QStackedWidget) component).count() == 0) {
            Iterator<String> ids = myLayoutInfos.keySet().iterator();
            if (ids.hasNext()) {
                swipeTo(ids.next());
            }
        }
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        ((QStackedWidget) myComponent).addWidget(child.toQtComponent());
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        QWidget widget = child.toQtComponent();
        if (widget != null && myComponent != null) {
            ((QStackedWidget) myComponent).removeWidget(widget);
        }
    }

    @Override
    public SwipeLayout register(String id, @RequiredUIAccess Supplier<Layout> layoutSupplier) {
        myLayoutInfos.put(id, new LayoutInfo(layoutSupplier));
        return this;
    }

    @Override
    @RequiredUIAccess
    public Layout swipeLeftTo(String id) {
        return swipeTo(id);
    }

    @Override
    @RequiredUIAccess
    public Layout swipeRightTo(String id) {
        return swipeTo(id);
    }

    /**
     * A stack has no direction of its own - the api asks to arrive at a page from one side or the other, and
     * both answer the same page.
     */
    @RequiredUIAccess
    private Layout swipeTo(String id) {
        LayoutInfo info = myLayoutInfos.get(id);
        if (info == null) {
            throw new IllegalArgumentException("There no layout with id: " + id);
        }

        Layout layout = info.get();

        if (!(layout instanceof QtComponentDelegate<?> delegate)) {
            return layout;
        }

        if (delegate.toQtComponent() == null) {
            addImpl(layout, id);
        }

        QWidget widget = delegate.toQtComponent();
        if (widget != null && myComponent != null) {
            ((QStackedWidget) myComponent).setCurrentWidget(widget);
        }

        return layout;
    }
}
