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
import consulo.ui.StaticPosition;
import consulo.ui.internal.LayoutVisualOrder;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Dummy-but-creatable headless {@link DockLayout}.
 *
 * @author VISTALL
 */
public class HeadlessDockLayout extends HeadlessLayoutBase<StaticPosition> implements DockLayout {
    private final Map<StaticPosition, Component> myPositions = new ConcurrentHashMap<>();

    @Override
    public Layout<StaticPosition> add(Component component, StaticPosition constraint) {
        Component previous = myPositions.put(constraint, component);
        if (previous == component) {
            return this;
        }
        if (previous != null) {
            super.remove(previous);
        }
        myPositions.entrySet().removeIf(entry -> entry.getKey() != constraint && entry.getValue() == component);
        super.remove(component);
        addChild(component);
        return this;
    }

    @Override
    public void remove(Component component) {
        myPositions.values().remove(component);
        super.remove(component);
    }

    @Override
    public void removeAll() {
        myPositions.clear();
        super.removeAll();
    }

    @Override
    public void forEachChild(Consumer<Component> consumer) {
        for (StaticPosition position : LayoutVisualOrder.DOCK_POSITIONS) {
            Component component = myPositions.get(position);
            if (component != null) {
                consumer.accept(component);
            }
        }
    }
}
