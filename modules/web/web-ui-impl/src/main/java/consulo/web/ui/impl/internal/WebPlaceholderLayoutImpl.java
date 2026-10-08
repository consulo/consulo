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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.html.Div;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class WebPlaceholderLayoutImpl extends VaadinComponentDelegate<WebPlaceholderLayoutImpl.Vaadin> implements PlaceholderLayout {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebPlaceholderLayoutImpl.this;
        }
    }

    private final Map<Layer, Div> mySlots = new EnumMap<>(Layer.class);
    private final Map<Layer, Component> myChildren = new EnumMap<>(Layer.class);

    private BooleanSupplier myPlaceholderVisibility = () -> false;
    private boolean myPlaceholderVisible;

    public WebPlaceholderLayoutImpl() {
        Vaadin vaadin = getVaadinComponent();
        vaadin.addClassName("web-placeholder-layout");

        for (Layer layer : Layer.values()) {
            Div slot = new Div();
            slot.addClassName("web-placeholder-layout-slot");
            mySlots.put(layer, slot);
            vaadin.add(slot);
        }

        showCurrentSlot();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Layout<Layer> add(Component component, Layer constraint) {
        Div slot = mySlots.get(constraint);
        slot.removeAll();
        slot.add(TargetVaadin.to(component));
        myChildren.put(constraint, component);
        return this;
    }

    @Override
    public void remove(Component component) {
        for (Layer layer : Layer.values()) {
            if (myChildren.get(layer) == component) {
                myChildren.remove(layer);
                mySlots.get(layer).removeAll();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        myChildren.clear();
        for (Div slot : mySlots.values()) {
            slot.removeAll();
        }
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        List<Component> children = new ArrayList<>(myChildren.values());
        children.forEach(consumer);
    }

    @Override
    @RequiredUIAccess
    public void setPlaceholderVisibility(BooleanSupplier booleanSupplier) {
        myPlaceholderVisibility = booleanSupplier;
        testPlaceholder();
    }

    @Override
    @RequiredUIAccess
    public boolean testPlaceholder() {
        boolean placeholderVisible = myPlaceholderVisibility.getAsBoolean();
        myPlaceholderVisible = placeholderVisible;
        showCurrentSlot();
        return placeholderVisible;
    }

    private void showCurrentSlot() {
        mySlots.get(Layer.PLACEHOLDER).setVisible(myPlaceholderVisible);
        mySlots.get(Layer.CONTENT).setVisible(!myPlaceholderVisible);
    }
}
