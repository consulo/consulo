/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.ui.impl.internal.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.provider.HasListDataView;
import consulo.ui.Length;
import consulo.ui.TextItemRender;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import consulo.util.collection.ContainerUtil;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebSingleListComponentBase<V, C extends Component & HasListDataView & HasValue & FromVaadinComponentWrapper>
    extends VaadinComponentDelegate<C> implements ValueComponent<V> {
    protected final FlatDataModel<V> myModel;
    protected TextItemRender<V> myTextRender = TextItemRender.defaultRender();

    protected @Nullable Function<V, String> mySpeedSearchConverter;
    protected @Nullable Function<V, Length> myItemHeightGetter;
    protected int myVisibleRowCount;

    @SuppressWarnings("unchecked")
    protected WebSingleListComponentBase(FlatDataModel<V> model) {
        myModel = model;
        C component = toVaadinComponent();
        pushItems();

        component.addValueChangeListener(
            event -> getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, event.getValue()))
        );

        // a vaadin list holds its own copy of the items, so any structural change has to be pushed back
        model.addListener(event -> pushItems());
    }

    /**
     * Hands the list every item it holds. This one builds a row for each of them up front, which is why a model that
     * may run long asks for the lazy list instead.
     */
    @SuppressWarnings("unchecked")
    protected void pushItems() {
        C component = toVaadinComponent();

        component.setItems(ContainerUtil.collect(myModel.iterator()));
    }

    public FlatDataModel<V> getDataModel() {
        return myModel;
    }

    public void setSpeedSearchConverter(@Nullable Function<V, String> converter) {
        mySpeedSearchConverter = converter;
    }

    public @Nullable String getSpeedSearchText() {
        return null;
    }

    public void setItemHeightGetter(@Nullable Function<V, Length> getter) {
        myItemHeightGetter = getter;

        // the rows were built by the renderer already, and a renderer is only run again when the items are pushed
        pushItems();
    }

    protected void applyItemHeight(com.vaadin.flow.component.Component itemComponent, @Nullable V item) {
        itemComponent.getElement().getStyle()
            .set("width", "100%")
            .set("max-width", "100%")
            .set("min-width", "0")
            .set("flex", "1 1 0")
            .set("overflow", "hidden")
            .set("box-sizing", "border-box");

        if (myItemHeightGetter != null && item != null) {
            itemComponent.getElement().getStyle().set("height", WebLength.toCss(myItemHeightGetter.apply(item)));
        }
    }

    /**
     * The browser has no row count to set, so the rows are bounded by how tall the list is allowed to grow, and what
     * scrolls the rest past it is the scroll layout the list is placed in - a list never scrolls its own content.
     * The height of one row is not known here, so it is taken from the item height when one was given and from the
     * theme otherwise.
     */
    public void setVisibleRowCount(int count) {
        myVisibleRowCount = count;

        // what reaches the browser is bounded by this, so the rows have to be pushed again against the new bound
        pushItems();

        com.vaadin.flow.component.Component component = toVaadinComponent();

        if (count <= 0) {
            component.getElement().getStyle().remove("max-height");
            return;
        }

        String rowHeight = myItemHeightGetter != null && myModel.getSize() > 0
            ? WebLength.toCss(myItemHeightGetter.apply(myModel.get(0)))
            : "var(--consulo-list-row-height, 24px)";

        // a height only - the list never scrolls anything itself, the scroll layout it is put in does
        component.getElement().getStyle().set("max-height", "calc(" + count + " * " + rowHeight + ")");
    }

    @RequiredUIAccess
    public void setValueByIndex(int index) {
        setValue(myModel.get(index));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable V getValue() {
        return (V) toVaadinComponent().getValue();
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void setValue(V value, boolean fireListeners) {
        getVaadinComponent().setValue(value);
    }
}
