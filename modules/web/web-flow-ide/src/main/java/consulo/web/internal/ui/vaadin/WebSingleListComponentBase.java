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
package consulo.web.internal.ui.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.provider.HasListDataView;
import consulo.ui.TextItemRender;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import consulo.util.collection.ContainerUtil;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebSingleListComponentBase<V, C extends Component & HasListDataView & HasValue & FromVaadinComponentWrapper>
    extends VaadinComponentDelegate<C> implements ValueComponent<V> {
    protected final FlatDataModel<V> myModel;
    protected TextItemRender<V> myTextRender = TextItemRender.defaultRender();

    protected @Nullable Function<V, String> mySpeedSearchConverter;
    protected @Nullable ToIntFunction<V> myItemHeightGetter;

    @SuppressWarnings("unchecked")
    protected WebSingleListComponentBase(FlatDataModel<V> model) {
        myModel = model;
        C component = toVaadinComponent();
        component.setItems(ContainerUtil.collect(model.iterator()));

        component.addValueChangeListener(
            event -> getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, event.getValue()))
        );

        // a vaadin list holds its own copy of the items, so any structural change has to be pushed back
        model.addListener(event -> component.setItems(ContainerUtil.collect(model.iterator())));
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

    public void setItemHeightGetter(@Nullable ToIntFunction<V> getter) {
        myItemHeightGetter = getter;
    }

    protected void applyItemHeight(com.vaadin.flow.component.Component itemComponent, @Nullable V item) {
        if (myItemHeightGetter != null && item != null) {
            itemComponent.getElement().getStyle().set("height", myItemHeightGetter.applyAsInt(item) + "px");
        }
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
