/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl.layout;

import consulo.ui.StaticPosition;
import consulo.ui.layout.DockLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.eclipse.swt.widgets.Layout;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtDockLayoutImpl extends DesktopSwtLayoutComponent<StaticPosition, BorderLayout.BorderData> implements DockLayout {
    public DesktopSwtDockLayoutImpl(int gapInPixels) {
    }

    @Nullable
    @Override
    protected Layout createLayout() {
        return new BorderLayout();
    }

    @Override
    public BorderLayout.BorderData convertConstraintsToLayoutData(@Nonnull StaticPosition constraint) {
        switch (constraint) {
            case TOP:
                return new BorderLayout.BorderData(BorderLayout.NORTH);
            case BOTTOM:
                return new BorderLayout.BorderData(BorderLayout.SOUTH);
            case LEFT:
                return new BorderLayout.BorderData(BorderLayout.WEST);
            case RIGHT:
                return new BorderLayout.BorderData(BorderLayout.EAST);
            case CENTER:
                return new BorderLayout.BorderData(BorderLayout.CENTER);
            default:
                throw new IllegalArgumentException(constraint.name());
        }
    }
}
