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
import consulo.ui.layout.LayoutConstraint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Base for dummy-but-creatable headless {@link Layout}s. It keeps the children it is given without arranging them.
 * {@link Layout#add} throws by default and the typed adds of {@code DockLayout}, {@code VerticalLayout} and the rest
 * route through it, so a layout which leaves it alone cannot be filled at all. Subclasses add their own positional or
 * named accessors on top.
 *
 * @author VISTALL
 */
public abstract class HeadlessLayoutBase<C extends LayoutConstraint> extends HeadlessComponentBase implements Layout<C> {
    private final List<Component> myChildren = new CopyOnWriteArrayList<>();

    /**
     * Records a child for a typed {@code add} which narrows the return type and therefore cannot route through
     * {@link #add(Component, LayoutConstraint)}.
     */
    protected void addChild(Component component) {
        myChildren.add(component);
    }

    @Override
    public Layout<C> add(Component component, C constraint) {
        addChild(component);
        return this;
    }

    @Override
    public void remove(Component component) {
        myChildren.remove(component);
    }

    @Override
    public void removeAll() {
        myChildren.clear();
    }

    @Override
    public void forEachChild(Consumer<Component> consumer) {
        myChildren.forEach(consumer);
    }
}
