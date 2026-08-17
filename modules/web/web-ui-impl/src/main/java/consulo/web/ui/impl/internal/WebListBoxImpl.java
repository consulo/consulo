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
package consulo.web.ui.impl.internal;

import consulo.ui.TransferHandler;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.ui.ComponentItemRender;
import com.vaadin.flow.component.html.Hr;
import consulo.ui.ListBox;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.model.FlatDataModel;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.vaadin.WebSingleListComponentBase;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
@SuppressWarnings("unchecked")
public class WebListBoxImpl<E> extends WebSingleListComponentBase<E, WebListBoxImpl.Vaadin> implements ListBox<E> {
    private static final String SEPARATOR_CLASS = "web-list-box-separator";

    private @Nullable TransferHandler<E> myTransferHandler;
    private Predicate<E> mySeparatorPredicate = item -> false;
    private @Nullable ComponentItemRender<E> myComponentRender;
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

    /**
     * The rows are the light dom children of the vaadin list, one per item in the order of the model - so which one
     * the pointer is over is the index of the row it entered, and selecting is left to the platform.
     */
    @Override
    public void setSelectOnHover(boolean selectOnHover) {
        toVaadinComponent().getElement().executeJs(
            """
            const list = this;
            if (list.$consuloHoverSelect) {
                list.$consuloHoverSelect = $0;
                return;
            }

            list.$consuloHoverSelect = $0;

            // the list opens under the pointer, and the event that arrives from standing still would take the
            // selection off whatever the step chose as its default - only a pointer which moved is following it
            let lastX = null;
            let lastY = null;

            list.addEventListener('mousemove', event => {
                if (!list.$consuloHoverSelect) {
                    return;
                }

                if (lastX === null || (lastX === event.clientX && lastY === event.clientY)) {
                    lastX = event.clientX;
                    lastY = event.clientY;
                    return;
                }

                lastX = event.clientX;
                lastY = event.clientY;

                const item = event.target.closest && event.target.closest('vaadin-list-box > *');
                if (!item || item.hasAttribute('disabled')) {
                    return;
                }

                const index = Array.prototype.indexOf.call(list.children, item);
                if (index >= 0 && index !== list.selected) {
                    list.selected = index;
                    list.dispatchEvent(new CustomEvent('selected-changed', { detail: { value: index } }));
                }
            });
            """,
            selectOnHover
        );
    }

    @Override
    public void isSeparator(Predicate<E> predicate) {
        mySeparatorPredicate = predicate;

        // a separator stands between the items rather than being one, so it is never a thing to land on
        toVaadinComponent().setItemEnabledProvider(item -> !predicate.test((E) item));

        // the rows are built when the renderer is set, so one already built knows nothing of this predicate
        applyRender();
    }

    private void applyRender() {
        if (myComponentRender != null) {
            setRender(myComponentRender);
        }
        else {
            setRender(myTextRender);
        }
    }

    /**
     * A line, until a separator carries anything of its own to draw.
     */
    private com.vaadin.flow.component.@Nullable Component separatorOrNull(@Nullable E item) {
        if (item == null || !mySeparatorPredicate.test(item)) {
            return null;
        }

        Hr line = new Hr();
        line.addClassName(SEPARATOR_CLASS);
        return line;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myTextRender = render;
        myComponentRender = null;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            com.vaadin.flow.component.Component separator = separatorOrNull((E) item);
            if (separator != null) {
                return separator;
            }

            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            render.render(presentation, RenderItem.of((E) item, isSelected((E) item)));

            com.vaadin.flow.component.Component component = presentation.toComponent();
            applyItemHeight(component, (E) item);
            return applyDoubleClick(component, (E) item);
        }));
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        myComponentRender = render;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            com.vaadin.flow.component.Component separator = separatorOrNull((E) item);
            if (separator != null) {
                return separator;
            }

            consulo.ui.Component rendered = render.render(RenderItem.of((E) item, isSelected((E) item)));

            com.vaadin.flow.component.Component component = ((ToVaadinComponentWrapper) rendered).toVaadinComponent();
            applyItemHeight(component, (E) item);
            return applyDoubleClick(component, (E) item);
        }));
    }

    private com.vaadin.flow.component.Component applyDoubleClick(com.vaadin.flow.component.Component component, @Nullable E item) {
        component.getElement().addEventListener(
            "dblclick",
            event -> getListenerDispatcher(ListDoubleClickEvent.class).onEvent(new ListDoubleClickEvent(this, item))
        );
        return component;
    }

    private boolean isSelected(@Nullable E item) {
        return item != null && item.equals(getValue());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<E> handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler<E> getTransferHandler() {
        return myTransferHandler;
    }
}
