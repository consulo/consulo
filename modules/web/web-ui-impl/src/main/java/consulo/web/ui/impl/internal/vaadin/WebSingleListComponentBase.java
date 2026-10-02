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
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebSingleListComponentBase<V, C extends Component & HasListDataView & HasValue & FromVaadinComponentWrapper>
    extends WebListComponentBase<V, C> implements ValueComponent<V> {
    @SuppressWarnings("unchecked")
    protected WebSingleListComponentBase(FlatDataModel<V> model) {
        super(model);

        toVaadinComponent().addValueChangeListener(
            event -> getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, event.getValue()))
        );
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
