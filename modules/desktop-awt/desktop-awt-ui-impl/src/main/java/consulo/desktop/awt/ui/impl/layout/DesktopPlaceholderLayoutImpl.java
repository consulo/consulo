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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.Layout;
import consulo.ui.layout.PlaceholderLayout;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class DesktopPlaceholderLayoutImpl extends DesktopLayoutBase<JPanel, PlaceholderLayout.Layer> implements PlaceholderLayout {
    private final CardLayout myCardLayout = new CardLayout();
    private final Map<Layer, JPanel> mySlots = new EnumMap<>(Layer.class);

    private BooleanSupplier myPlaceholderVisibility = () -> false;
    private boolean myPlaceholderVisible;

    public DesktopPlaceholderLayoutImpl() {
        initDefaultPanel(myCardLayout);
    }

    @Override
    protected JPanel createComponent() {
        JPanel panel = super.createComponent();
        for (Layer layer : Layer.values()) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);
            mySlots.put(layer, slot);
            panel.add(slot, layer.name());
        }
        myCardLayout.show(panel, (myPlaceholderVisible ? Layer.PLACEHOLDER : Layer.CONTENT).name());
        return panel;
    }

    @Override
    public Layout<Layer> add(Component component, Layer constraint) {
        JPanel slot = slot(constraint);
        slot.removeAll();
        slot.add(TargetAWT.to(component), BorderLayout.CENTER);

        JPanel panel = toAWTComponent();
        panel.revalidate();
        panel.repaint();
        return this;
    }

    @Override
    public void remove(Component component) {
        java.awt.Component awtComponent = TargetAWT.to(component);
        for (JPanel slot : slots()) {
            if (awtComponent.getParent() == slot) {
                slot.remove(awtComponent);
                slot.revalidate();
                slot.repaint();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        for (JPanel slot : slots()) {
            slot.removeAll();
        }

        JPanel panel = toAWTComponent();
        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        for (JPanel slot : slots()) {
            for (java.awt.Component child : slot.getComponents()) {
                if (child instanceof FromSwingComponentWrapper wrapper) {
                    consumer.accept(wrapper.toUIComponent());
                }
            }
        }
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

        JPanel panel = toAWTComponent();
        myCardLayout.show(panel, (placeholderVisible ? Layer.PLACEHOLDER : Layer.CONTENT).name());
        panel.revalidate();
        panel.repaint();
        return placeholderVisible;
    }

    private JPanel slot(Layer layer) {
        toAWTComponent();
        return mySlots.get(layer);
    }

    private Iterable<JPanel> slots() {
        toAWTComponent();
        return mySlots.values();
    }
}
