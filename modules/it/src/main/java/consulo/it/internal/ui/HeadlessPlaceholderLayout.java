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
package consulo.it.internal.ui;

import consulo.ui.Component;
import consulo.ui.layout.Layout;
import consulo.ui.layout.PlaceholderLayout;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class HeadlessPlaceholderLayout extends HeadlessLayoutBase<PlaceholderLayout.Layer> implements PlaceholderLayout {
    private final Map<Layer, Component> myChildren = new EnumMap<>(Layer.class);

    private BooleanSupplier myPlaceholderVisibility = () -> false;

    @Override
    public Layout<Layer> add(Component component, Layer constraint) {
        Component old = myChildren.put(constraint, component);
        if (old != null) {
            super.remove(old);
        }
        return super.add(component, constraint);
    }

    @Override
    public void remove(Component component) {
        myChildren.values().remove(component);
        super.remove(component);
    }

    @Override
    public void removeAll() {
        myChildren.clear();
        super.removeAll();
    }

    @Override
    public void setPlaceholderVisibility(BooleanSupplier booleanSupplier) {
        myPlaceholderVisibility = booleanSupplier;
    }

    @Override
    public boolean testPlaceholder() {
        return myPlaceholderVisibility.getAsBoolean();
    }
}
