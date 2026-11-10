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
package consulo.ui.impl.table;

import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The items of a table whose details are shown, each with the component the renderer gave for it. Every change
 * answers the components it dropped, so a frontend can take them down.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class TableDetails<Item> {
    private @Nullable Function<Item, Component> myRenderer;
    private final Map<Item, Component> myShown = new LinkedHashMap<>();

    public List<Component> setRenderer(@Nullable Function<Item, Component> renderer) {
        myRenderer = renderer;
        List<Component> dropped = new ArrayList<>(myShown.values());
        myShown.clear();
        return dropped;
    }

    public @Nullable Component show(Item item) {
        Component shown = myShown.get(item);
        if (shown != null) {
            return shown;
        }

        Function<Item, Component> renderer = myRenderer;
        if (renderer == null) {
            return null;
        }

        Component component = renderer.apply(item);
        myShown.put(item, component);
        return component;
    }

    public @Nullable Component hide(Item item) {
        return myShown.remove(item);
    }

    public List<Component> retain(Predicate<Item> present) {
        List<Component> dropped = new ArrayList<>();
        for (Iterator<Map.Entry<Item, Component>> iterator = myShown.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Item, Component> entry = iterator.next();
            if (!present.test(entry.getKey())) {
                dropped.add(entry.getValue());
                iterator.remove();
            }
        }
        return dropped;
    }

    public boolean isShown(Item item) {
        return myShown.containsKey(item);
    }

    public @Nullable Component get(Item item) {
        return myShown.get(item);
    }

    public boolean isEmpty() {
        return myShown.isEmpty();
    }

    public List<Item> getItems() {
        return List.copyOf(myShown.keySet());
    }
}
