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

import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Space;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.VerticalLayout;
import io.qt.core.Qt;
import io.qt.widgets.QBoxLayout;
import io.qt.widgets.QVBoxLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtVerticalLayoutImpl extends DesktopQtBoxLayoutComponent<LayoutConstraint> implements VerticalLayout {
    private final Space myVGap;
    private final @Nullable HorizontalAlignment myAlignment;

    public DesktopQtVerticalLayoutImpl(Space vGap) {
        this(vGap, null);
    }

    public DesktopQtVerticalLayoutImpl(Space vGap, @Nullable HorizontalAlignment alignment) {
        myVGap = vGap;
        myAlignment = alignment;
    }

    @Override
    protected QBoxLayout createBoxLayout() {
        QVBoxLayout layout = new QVBoxLayout();
        layout.setSpacing(DesktopQtSpace.toPixels(myVGap));
        return layout;
    }

    /**
     * An alignment shrinks every child to the width it asks for, the way the web frontend answers one - a layout
     * built without an alignment keeps stretching its children over the whole width instead.
     */
    @Override
    protected Qt.Alignment itemAlignment() {
        if (myAlignment == null) {
            return super.itemAlignment();
        }

        return switch (myAlignment) {
            case LEFT -> new Qt.Alignment(Qt.AlignmentFlag.AlignLeft);
            case CENTER -> new Qt.Alignment(Qt.AlignmentFlag.AlignHCenter);
            case RIGHT -> new Qt.Alignment(Qt.AlignmentFlag.AlignRight);
        };
    }
}
