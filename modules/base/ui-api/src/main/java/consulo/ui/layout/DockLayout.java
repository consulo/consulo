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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIConstant;
import consulo.ui.internal.UIInternal;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface DockLayout extends Layout<StaticPosition> {
    @Nonnull
    static DockLayout create() {
        return create(UIConstant.DEFAULT_SPACING_PX);
    }

    @Nonnull
    static DockLayout create(int gapInPixels) {
        return UIInternal.get()._Layouts_dock(gapInPixels);
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout top(@Nonnull PseudoComponent component) {
        return top(component.getComponent());
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout bottom(@Nonnull PseudoComponent component) {
        return bottom(component.getComponent());
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout center(@Nonnull PseudoComponent component) {
        return center(component.getComponent());
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout left(@Nonnull PseudoComponent component) {
        return left(component.getComponent());
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout right(@Nonnull PseudoComponent component) {
        return right(component.getComponent());
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout top(@Nonnull Component component) {
        return (DockLayout) add(component, StaticPosition.TOP);
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout bottom(@Nonnull Component component) {
        return (DockLayout) add(component, StaticPosition.BOTTOM);
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout center(@Nonnull Component component) {
        return (DockLayout) add(component, StaticPosition.CENTER);
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout left(@Nonnull Component component) {
        return (DockLayout) add(component, StaticPosition.LEFT);
    }

    @Nonnull
    @RequiredUIAccess
    default DockLayout right(@Nonnull Component component) {
        return (DockLayout) add(component, StaticPosition.RIGHT);
    }
}
