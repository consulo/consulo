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

import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.TextItemRenderer;
import consulo.ui.model.ListModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.vaadin.WebSingleListComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
@SuppressWarnings("unchecked")
public class WebListBoxImpl<E> extends WebSingleListComponentBase<E, WebListBoxImpl.Vaadin> implements ListBox<E> {
    public class Vaadin extends com.vaadin.flow.component.listbox.ListBox<E> implements FromVaadinComponentWrapper {
        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return WebListBoxImpl.this;
        }
    }

    public WebListBoxImpl(ListModel<E> model) {
        super(model);

        setRenderer(TextItemRenderer.defaultRenderer());
    }

    @Override
    public void setRenderer(TextItemRenderer<E> renderer) {
        toVaadinComponent().setRenderer(new ComponentRenderer((item) -> {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            renderer.render(presentation, myModel.indexOf((E) item), (E) item);
            return presentation.toComponent();
        }));
    }

    @Override
    public ListModel<E> getListModel() {
        return myModel;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
