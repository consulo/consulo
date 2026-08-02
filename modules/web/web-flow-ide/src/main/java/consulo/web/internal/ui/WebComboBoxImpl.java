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
package consulo.web.internal.ui;

import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.model.FlatDataModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.ToVaadinComponentWrapper;
import consulo.web.internal.ui.vaadin.WebSingleListComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
@SuppressWarnings("unchecked")
public class WebComboBoxImpl<V> extends WebSingleListComponentBase<V, WebComboBoxImpl.Vaadin> implements ComboBox<V> {
    public class Vaadin extends Select<V> implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebComboBoxImpl.this;
        }
    }

    public WebComboBoxImpl(FlatDataModel<V> model) {
        super(model);

        setRender(TextItemRender.defaultRender());
    }

    @Override
    public void setRender(TextItemRender<V> render) {
        myTextRender = render;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            render.render(presentation, RenderItem.of((V) item, isSelected((V) item)));

            com.vaadin.flow.component.Component component = presentation.toComponent();
            applyItemHeight(component, (V) item);
            return component;
        }));
    }

    @Override
    public void setRender(ComponentItemRender<V> render) {
        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            Component rendered = render.render(RenderItem.of((V) item, isSelected((V) item)));

            com.vaadin.flow.component.Component component = ((ToVaadinComponentWrapper) rendered).toVaadinComponent();
            applyItemHeight(component, (V) item);
            return component;
        }));
    }

    private boolean isSelected(@Nullable V item) {
        return item != null && item.equals(getValue());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
