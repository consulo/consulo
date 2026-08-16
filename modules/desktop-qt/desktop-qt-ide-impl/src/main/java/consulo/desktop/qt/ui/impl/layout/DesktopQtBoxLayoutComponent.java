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
import consulo.ui.layout.LayoutConstraint;
import io.qt.core.Qt;
import io.qt.widgets.QBoxLayout;
import io.qt.widgets.QLayout;
import org.jspecify.annotations.Nullable;

/**
 * Base of the two box layouts of the api, which pack their children at the start and leave the remainder of the
 * container empty - the same way the web frontend behaves with its flex layouts.
 * <p/>
 * A bare {@link QBoxLayout} does the opposite and hands the leftover space to the items, so a trailing stretch is
 * kept as the last item of the layout and every child is inserted in front of it.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public abstract class DesktopQtBoxLayoutComponent<C extends LayoutConstraint> extends DesktopQtLayoutComponent<C, Object> {
    @Override
    protected final QLayout createLayout() {
        QBoxLayout layout = createBoxLayout();
        layout.addStretch();
        return layout;
    }

    protected abstract QBoxLayout createBoxLayout();

    /**
     * How every child is placed on the axis the layout does not fill. No flag at all - the default - hands the
     * child the whole of that axis, which is what a layout built without an alignment answers in the web frontend.
     */
    protected Qt.Alignment itemAlignment() {
        return new Qt.Alignment();
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        QBoxLayout layout = (QBoxLayout) myComponent.layout();

        layout.insertWidget(Math.max(0, layout.count() - 1), child.toQtComponent(), 0, itemAlignment());
    }
}
