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

import consulo.ui.*;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import consulo.localize.LocalizeValue;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.event.ClickEvent;
import consulo.ui.internal.PlainTextPresentation;
import consulo.ui.model.FlatDataModel;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.WebInputDetails;
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
    private static final String SEPARATOR_CAPTION_CLASS = "web-list-box-separator-caption";
    private static final String HOVER_SELECT_CLASS = "web-list-box-hover-select";

    private @Nullable TransferHandler<E> myTransferHandler;
    private Predicate<E> mySeparatorPredicate = item -> false;
    private @Nullable ComponentItemRenderer<E> myComponentRenderer;
    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips rebuilding
    // on css only changes
    @StyleSheet("/list/webListBox.css")
    public class Vaadin extends com.vaadin.flow.component.listbox.ListBox<E> implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebListBoxImpl.this;
        }
    }

    public WebListBoxImpl(FlatDataModel<E> model) {
        super(model);

        toVaadinComponent().addClassName("web-list-box");

        setRenderer(TextItemRenderer.defaultRenderer());

        myClickInstalled = true;
    }

    /**
     * The rows are the light dom children of the vaadin list, one per item in the order of the model - so which one
     * the pointer is over is the index of the row it entered, and selecting is left to the platform.
     */
    @Override
    public void setSelectOnHover(boolean selectOnHover) {
        toVaadinComponent().getClassNames().set(HOVER_SELECT_CLASS, selectOnHover);

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

                if (list.contains(document.activeElement) && document.activeElement !== item) {
                    item.focus({ preventScroll: true });
                }
            });

            list.addEventListener('focusin', event => {
                if (!list.$consuloHoverSelect) {
                    return;
                }

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
        applyRenderer();
    }

    protected void applyRenderer() {
        if (myComponentRenderer != null) {
            setRenderer(myComponentRenderer);
        }
        else {
            setRenderer(myTextRenderer);
        }
    }

    /**
     * A line, with the text the renderer gives the separator in front of it as its caption.
     */
    private com.vaadin.flow.component.@Nullable Component separatorOrNull(@Nullable E item) {
        if (item == null || !mySeparatorPredicate.test(item)) {
            return null;
        }

        Hr line = new Hr();
        line.addClassName(SEPARATOR_CLASS);

        String caption = separatorCaption(item);
        if (caption.isEmpty()) {
            return line;
        }

        Div separator = new Div(new Span(caption), line);
        separator.addClassName(SEPARATOR_CAPTION_CLASS);
        return separator;
    }

    private String separatorCaption(E item) {
        TextItemRenderer<E> renderer = myComponentRenderer == null ? myTextRenderer : null;
        if (renderer == null) {
            return "";
        }
        return PlainTextPresentation.textOf(presentation -> renderer.render(presentation, RenderItem.of(item, false))).get().trim();
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        getVaadinComponent().getElement().setAttribute("placeholder", text.get());
    }

    @Override
    public void setRenderer(TextItemRenderer<E> renderer) {
        myTextRenderer = renderer;
        myComponentRenderer = null;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            com.vaadin.flow.component.Component separator = separatorOrNull((E) item);
            if (separator != null) {
                return separator;
            }

            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            renderer.render(presentation, RenderItem.of((E) item, isSelected((E) item)));

            com.vaadin.flow.component.Component component = decorateRow(presentation.toComponent(), (E) item);
            String background = WebColors.toCssColor(presentation.getBackgroundColor());
            if (background != null) {
                component.getElement().getStyle().set("background-color", background);
            }
            applyItemHeight(component, (E) item);
            return applyDoubleClick(component, (E) item);
        }));
    }

    @Override
    public void setRenderer(ComponentItemRenderer<E> renderer) {
        myComponentRenderer = renderer;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            com.vaadin.flow.component.Component separator = separatorOrNull((E) item);
            if (separator != null) {
                return separator;
            }

            Component rendered = renderer.render(RenderItem.of((E) item, isSelected((E) item)));

            com.vaadin.flow.component.Component component = decorateRow(((ToVaadinComponentWrapper) rendered).toVaadinComponent(), (E) item);
            applyItemHeight(component, (E) item);
            return applyDoubleClick(component, (E) item);
        }));
    }

    protected com.vaadin.flow.component.Component decorateRow(com.vaadin.flow.component.Component row, @Nullable E item) {
        return row;
    }

    private com.vaadin.flow.component.Component applyDoubleClick(com.vaadin.flow.component.Component component, @Nullable E item) {
        WebInputDetails.addClickListener(
            component.getElement(),
            "dblclick",
            details -> getListenerDispatcher(ListDoubleClickEvent.class).onEvent(new ListDoubleClickEvent(this, item, details))
        );

        // the press is answered by the row rather than by the list, so the whole row answers it - the padding a row
        // carries is part of it - and the item pressed is known outright instead of being read back from a selection
        // which a press landing between rows would leave standing at the row chosen before
        WebInputDetails.addClickListener(
            component.getElement(),
            details -> {
                if (item != null) {
                    setValue(item, true);
                }

                getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, details));
            }
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
