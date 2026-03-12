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

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface DockLayout extends Layout<StaticPosition> {
    static DockLayout create() {
        return create(UIConstant.DEFAULT_SPACING_PX);
    }
    static DockLayout create(int gapInPixels) {
        return UIInternal.get()._Layouts_dock(gapInPixels);
    }
    @RequiredUIAccess
    default DockLayout top(PseudoComponent component) {
        return top(component.getComponent());
    }
    @RequiredUIAccess
    default DockLayout bottom(PseudoComponent component) {
        return bottom(component.getComponent());
    }
    @RequiredUIAccess
    default DockLayout center(PseudoComponent component) {
        return center(component.getComponent());
    }
    @RequiredUIAccess
    default DockLayout left(PseudoComponent component) {
        return left(component.getComponent());
    }
    @RequiredUIAccess
    default DockLayout right(PseudoComponent component) {
        return right(component.getComponent());
    }
    @RequiredUIAccess
    default DockLayout top(Component component) {
        return (DockLayout) add(component, StaticPosition.TOP);
    }
    @RequiredUIAccess
    default DockLayout bottom(Component component) {
        return (DockLayout) add(component, StaticPosition.BOTTOM);
    }
    @RequiredUIAccess
    default DockLayout center(Component component) {
        return (DockLayout) add(component, StaticPosition.CENTER);
    }
    @RequiredUIAccess
    default DockLayout left(Component component) {
        return (DockLayout) add(component, StaticPosition.LEFT);
    }
    @RequiredUIAccess
    default DockLayout right(Component component) {
        return (DockLayout) add(component, StaticPosition.RIGHT);
    }
}
