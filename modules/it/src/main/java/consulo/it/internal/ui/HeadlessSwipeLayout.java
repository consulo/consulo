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
import consulo.ui.layout.SwipeLayout;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Headless {@link SwipeLayout}. A page is built the first time it is shown - the first registered one at once - and
 * stays a child from then on, as on the rendering frontends.
 *
 * @author VISTALL
 */
public class HeadlessSwipeLayout extends HeadlessLayoutBase<LayoutConstraint> implements SwipeLayout {
    private final Map<String, Supplier<Layout>> mySuppliers = new HashMap<>();
    private final Map<String, Layout> myBuilt = new HashMap<>();

    private @Nullable Layout myShown;

    @Override
    public SwipeLayout register(String id, Supplier<Layout> layoutSupplier) {
        mySuppliers.put(id, layoutSupplier);

        if (myShown == null) {
            show(id);
        }
        return this;
    }

    @Override
    public Layout swipeLeftTo(String id) {
        return show(id);
    }

    @Override
    public Layout swipeRightTo(String id) {
        return show(id);
    }

    public @Nullable Layout getShown() {
        return myShown;
    }

    @Override
    public void remove(Component component) {
        myBuilt.values().remove(component);
        if (myShown == component) {
            myShown = null;
        }
        super.remove(component);
    }

    @Override
    public void removeAll() {
        mySuppliers.clear();
        myBuilt.clear();
        myShown = null;
        super.removeAll();
    }

    private Layout show(String id) {
        Layout layout = myBuilt.get(id);
        if (layout == null) {
            Supplier<Layout> supplier = mySuppliers.get(id);
            if (supplier == null) {
                throw new IllegalArgumentException(id + " is not registered");
            }

            layout = supplier.get();
            myBuilt.put(id, layout);
            addChild(layout);
        }

        myShown = layout;
        return layout;
    }
}
