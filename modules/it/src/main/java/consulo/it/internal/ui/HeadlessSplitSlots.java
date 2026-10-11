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
import consulo.ui.layout.LayoutConstraint;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Base of the headless split layouts: a fixed row of slots, each holding at most one component, visited in slot order.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public abstract class HeadlessSplitSlots extends HeadlessLayoutBase<LayoutConstraint> {
    private final @Nullable Component[] mySlots;

    protected HeadlessSplitSlots(int slotCount) {
        mySlots = new Component[slotCount];
    }

    protected void setSlot(int slot, @Nullable Component component) {
        Component old = mySlots[slot];
        if (old == component) {
            return;
        }

        mySlots[slot] = component;

        if (old != null) {
            super.remove(old);
        }

        if (component != null) {
            addChild(component);
        }
    }

    public @Nullable Component getSlot(int slot) {
        return mySlots[slot];
    }

    @Override
    public void remove(Component component) {
        for (int i = 0; i < mySlots.length; i++) {
            if (mySlots[i] == component) {
                mySlots[i] = null;
            }
        }
        super.remove(component);
    }

    @Override
    public void removeAll() {
        Arrays.fill(mySlots, null);
        super.removeAll();
    }

    @Override
    public void forEachChild(Consumer<Component> consumer) {
        for (Component component : mySlots.clone()) {
            if (component != null) {
                consumer.accept(component);
            }
        }
    }
}
