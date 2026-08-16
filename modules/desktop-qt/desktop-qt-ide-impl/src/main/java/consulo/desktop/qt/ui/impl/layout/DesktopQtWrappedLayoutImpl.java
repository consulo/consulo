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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.WrappedLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QVBoxLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtWrappedLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements WrappedLayout {
    @Override
    protected boolean isSingleChild() {
        return true;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return new QVBoxLayout();
    }

    @Override
    @RequiredUIAccess
    public WrappedLayout set(@Nullable Component component) {
        addImpl(component, null);
        return this;
    }
}
