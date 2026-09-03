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
import consulo.ui.HorizontalAlignment;
import consulo.ui.PseudoComponent;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 2016-06-11
 */
public interface VerticalLayout extends Layout<LayoutConstraint> {
    static VerticalLayout create() {
        return create(Space.MEDIUM);
    }

    static VerticalLayout create(Space vGap) {
        return UIInternal.get()._Layouts_vertical(vGap);
    }

    /**
     * Without an alignment every child is given the width of the layout. With one the children keep their own
     * width and are placed at that side of it.
     */
    static VerticalLayout create(Space vGap, HorizontalAlignment alignment) {
        return UIInternal.get()._Layouts_vertical(vGap, alignment);
    }

    @RequiredUIAccess
    default VerticalLayout add(PseudoComponent component) {
        return add(component.getComponent());
    }

    @RequiredUIAccess
    default VerticalLayout add(Component component) {
        return (VerticalLayout) add(component, LayoutConstraint.NONE);
    }
}
