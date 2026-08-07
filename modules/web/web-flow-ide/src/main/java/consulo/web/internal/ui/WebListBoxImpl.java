/*
 * Copyright 2013-2023 consulo.io
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

import consulo.ui.TransferHandler;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.ui.ComponentItemRender;
import consulo.ui.ListBox;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.model.FlatDataModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.ToVaadinComponentWrapper;
import consulo.web.internal.ui.vaadin.WebSingleListComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
@SuppressWarnings("unchecked")
public class WebListBoxImpl<E> extends WebSingleListComponentBase<E, WebListBoxImpl.Vaadin> implements ListBox<E> {
    private @Nullable TransferHandler myTransferHandler;
    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips rebuilding
    // on css only changes
    @StyleSheet("/list/webListBox.css")
    public class Vaadin extends com.vaadin.flow.component.listbox.ListBox<E> implements FromVaadinComponentWrapper {
        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return WebListBoxImpl.this;
        }
    }

    public WebListBoxImpl(FlatDataModel<E> model) {
        super(model);

        toVaadinComponent().addClassName("web-list-box");

        setRender(TextItemRender.defaultRender());
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myTextRender = render;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            render.render(presentation, RenderItem.of((E) item, isSelected((E) item)));

            com.vaadin.flow.component.Component component = presentation.toComponent();
            applyItemHeight(component, (E) item);
            return component;
        }));
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            consulo.ui.Component rendered = render.render(RenderItem.of((E) item, isSelected((E) item)));

            com.vaadin.flow.component.Component component = ((ToVaadinComponentWrapper) rendered).toVaadinComponent();
            applyItemHeight(component, (E) item);
            return component;
        }));
    }

    private boolean isSelected(@Nullable E item) {
        return item != null && item.equals(getValue());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler getTransferHandler() {
        return myTransferHandler;
    }
}
