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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.components.MultiSelectComboBox;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopMultiSelectComboBoxImpl<E> extends DesktopComboBoxBaseImpl<E, DesktopMultiSelectComboBoxImpl<E>.MyMultiSelectComboBox>
    implements consulo.ui.MultiSelectComboBox<E> {
    class MyMultiSelectComboBox extends MultiSelectComboBox<E> implements FromSwingComponentWrapper {
        MyMultiSelectComboBox(List<E> items) {
            super(items);
        }

        @Override
        protected java.awt.Component createItemComponent(E item) {
            ComponentItemRender<E> componentRender = myComponentRender;
            if (componentRender != null) {
                return TargetAWT.to(componentRender.render(RenderItem.of(item, true)));
            }

            SimpleColoredComponent component = new SimpleColoredComponent();
            component.setOpaque(false);
            component.setIpad(JBUI.emptyInsets());
            myTextRender.render(new DesktopTextItemPresentationImpl(component), RenderItem.of(item, true));
            return component;
        }

        @Override
        public Component toUIComponent() {
            return DesktopMultiSelectComboBoxImpl.this;
        }
    }

    private List<E> myLastValue = List.of();
    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private @Nullable Function<List<E>, LocalizeValue> mySummaryRenderer;

    DesktopMultiSelectComboBoxImpl(FlatDataModel<E> model) {
        super(model);

        model.addListener(event -> onModelChanged());
    }

    @Override
    protected MyMultiSelectComboBox createComponent() {
        MyMultiSelectComboBox component = new MyMultiSelectComboBox(items());
        initComboBox(component.getComboBox());
        component.setPlaceholder(myPlaceholder.getNullIfEmpty());
        component.setSummary(toSummary(mySummaryRenderer));
        component.addActionListener(event -> fireIfChanged(DesktopAWTInputDetails.currentEvent(component)));
        return component;
    }

    @Override
    protected JComboBox<E> getComboBox(MyMultiSelectComboBox component) {
        return component.getComboBox();
    }

    @Override
    protected void onRenderChanged(MyMultiSelectComboBox component) {
        component.updateSelectedDisplay();
    }

    @RequiredUIAccess
    private void onModelChanged() {
        if (!isInitialized()) {
            return;
        }

        toAWTComponent().setItems(items());

        fireIfChanged(null);
    }

    private List<E> items() {
        List<E> items = new ArrayList<>(myModel.getSize());
        for (E item : myModel) {
            items.add(item);
        }
        return items;
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;
        if (isInitialized()) {
            toAWTComponent().setPlaceholder(text.getNullIfEmpty());
        }
    }

    @Override
    public void setSummaryRenderer(Function<List<E>, LocalizeValue> renderer) {
        mySummaryRenderer = renderer;
        if (isInitialized()) {
            toAWTComponent().setSummary(toSummary(renderer));
        }
    }

    private static <E> @Nullable Function<List<E>, String> toSummary(@Nullable Function<List<E>, LocalizeValue> renderer) {
        return renderer == null ? null : items -> renderer.apply(items).get();
    }

    @Override
    public List<E> getValue() {
        Set<E> selection = toAWTComponent().getSelectedItems();
        if (selection.isEmpty()) {
            return List.of();
        }

        List<E> values = new ArrayList<>(selection.size());
        for (E item : myModel) {
            if (selection.contains(item)) {
                values.add(item);
            }
        }
        return values;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        toAWTComponent().setSelectedItems(value);

        if (fireListeners) {
            fireIfChanged(null);
        }
        else {
            myLastValue = getValue();
        }
    }

    @RequiredUIAccess
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireIfChanged(@Nullable InputDetails inputDetails) {
        List<E> value = getValue();
        if (value.equals(myLastValue)) {
            return;
        }

        myLastValue = value;

        ValueComponentEvent<List<E>> event = inputDetails == null
            ? new ValueComponentEvent<>(this, value)
            : new ValueComponentEvent<>(this, value, inputDetails);
        getListenerDispatcher(ValueComponentEvent.class).onEvent(event);
    }
}
