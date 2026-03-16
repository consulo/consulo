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
import consulo.ui.HasComponentStyle;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface Layout<C extends LayoutConstraint> extends Component, HasComponentStyle<LayoutStyle> {
    default Layout<C> add(Component component, C constraint) {
        throw new UnsupportedOperationException("Adding not supported");
    }

    @RequiredUIAccess
    default void removeAll() {
        throw new AbstractMethodError(getClass().getName());
    }

    default void remove(Component component) {
        throw new AbstractMethodError(getClass().getName());
    }

    @RequiredUIAccess
    @Override
    default void setEnabledRecursive(boolean value) {
        setEnabled(value);

        forEachChild(component -> component.setEnabledRecursive(value));
    }

    default void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        throw new AbstractMethodError(getClass().getName());
    }
}
