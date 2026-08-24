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
package consulo.web.ui.impl.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.Length;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import consulo.ui.TransferHandler;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.ReusableComponentItemRender;
import consulo.ui.ListBox;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import consulo.ui.model.LazyFlatDataModel;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A list which only builds the rows it shows.
 * <p/>
 * A plain list box holds every row in the document at once, so a few hundred completions cost a pause before the
 * popup appears and a browser laying out rows nobody will look at. A virtual list asks for the rows around the
 * viewport and no more, which is what a lazy model is for.
 * <p/>
 * Selection is missing from the component - it renders and nothing else - so it is kept here, drawn by marking the
 * chosen row and read back the same way a list box's value is.
 *
 * @author VISTALL
 */
@SuppressWarnings("unchecked")
public class WebLazyListBoxImpl<E> extends VaadinComponentDelegate<WebLazyListBoxImpl.Vaadin> implements ListBox<E> {
    private @Nullable TransferHandler<E> myTransferHandler;
    // the same stylesheet the eager list uses, so a row looks the same whichever of them drew it
    @StyleSheet("/list/webListBox.css")
    public class Vaadin extends VirtualList<E> implements FromVaadinComponentWrapper {
        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return WebLazyListBoxImpl.this;
        }
    }

    private static final String UI_COMPONENT = "consulo.row.component";
    private static final String ITEM = "consulo.row.item";

    private final FlatDataModel<E> myModel;

    private @Nullable E myValue;
    private @Nullable Function<E, Length> myItemHeightGetter;
    private @Nullable Function<E, String> mySpeedSearchConverter;

    public WebLazyListBoxImpl(FlatDataModel<E> model) {
        myModel = model;

        toVaadinComponent().addClassName("web-list-box");

        setDataProvider();
        setRender(TextItemRender.defaultRender());

        model.addListener(this::onModelChanged);
    }

    @SuppressWarnings("unchecked")
    private void setDataProvider() {
        Vaadin list = toVaadinComponent();

        if (myModel instanceof LazyFlatDataModel<E> lazy) {
            list.setDataProvider(new CallbackDataProvider<E, Void>(
                query -> lazy.fetch(query.getOffset(), query.getLimit()).stream(),
                query -> myModel.getSize()
            ));
            return;
        }

        list.setDataProvider(DataProvider.fromCallbacks(
            query -> {
                int size = myModel.getSize();
                int from = Math.min(query.getOffset(), size);
                int to = Math.min(from + query.getLimit(), size);

                List<E> page = new ArrayList<>(to - from);
                for (int i = from; i < to; i++) {
                    page.add(myModel.get(i));
                }
                return page.stream();
            },
            query -> myModel.getSize()
        ));
    }

    /**
     * A row which only changed in place is redrawn on its own - a completion which finished working out how it renders
     * touches one item, and refreshing everything would rebuild the rows the user is reading.
     */
    @RequiredUIAccess
    private void onModelChanged(FlatDataModelEvent event) {
        if (event.getType() == FlatDataModelEvent.Type.UPDATED && event.getFromIndex() == event.getToIndex()) {
            int index = event.getFromIndex();
            if (index >= 0 && index < myModel.getSize()) {
                toVaadinComponent().getDataProvider().refreshItem(myModel.get(index));
                return;
            }
        }

        refreshItems();
    }

    /**
     * Redraws one row, if it is still one of the model's - an item which has just been filtered out has no row left
     * to redraw.
     */
    @RequiredUIAccess
    private void refreshRow(@Nullable E item) {
        if (item != null && myModel.indexOf(item) >= 0) {
            toVaadinComponent().getDataProvider().refreshItem(item);
        }
    }

    @RequiredUIAccess
    private void refreshItems() {
        toVaadinComponent().getDataProvider().refreshAll();
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            render.render(presentation, RenderItem.of((E) item, isSelected((E) item)));

            return decorate(presentation.toComponent(), (E) item);
        }));
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        // a render which says it may be reused is given a row per slot and asked to rebind it, rather than being
        // asked for a new one per item - a list which changes on every typed character is otherwise a fresh tree
        // of components per visible row over the wire, which is what a completion popup felt like
        if (render instanceof ReusableComponentItemRender<E, ?> reusable) {
            toVaadinComponent().setRenderer(reusableRenderer(reusable));
            return;
        }

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            Component rendered = render.render(RenderItem.of((E) item, isSelected((E) item)));

            return decorate(((ToVaadinComponentWrapper) rendered).toVaadinComponent(), (E) item);
        }));
    }

    /**
     * The reuse overload, and it has to be this one. {@code ComponentRenderer} keeps the rebinding half only when it
     * is handed a {@code BiFunction}; given a {@code BiConsumer} it leaves {@code componentUpdateFunction} null, and
     * {@code updateComponent} then falls through to {@code createComponent} - so a render that says it may be reused
     * was still built from nothing on every refresh, and the pooling here bought exactly nothing.
     */
    private <C extends Component> ComponentRenderer<com.vaadin.flow.component.Component, E> reusableRenderer(
        ReusableComponentItemRender<E, C> render
    ) {
        return new ComponentRenderer<>(
            item -> {
                C created = render.createComponent();
                com.vaadin.flow.component.Component vaadin = ((ToVaadinComponentWrapper) created).toVaadinComponent();

                // the row a component stands for changes as the list scrolls, so the press reads it when it fires
                ComponentUtil.setData(vaadin, UI_COMPONENT, created);
                listenForSelection(vaadin);

                // a fresh row is bound here too - creating is the first binding, not a step before one
                bindRow(render, vaadin, item);
                return vaadin;
            },
            (vaadin, item) -> {
                bindRow(render, vaadin, item);
                return vaadin;
            }
        );
    }

    /**
     * Sets everything a row shows. The row handed over is whatever the list last put there, never a clean one.
     */
    private <C extends Component> void bindRow(
        ReusableComponentItemRender<E, C> render,
        com.vaadin.flow.component.Component vaadin,
        E item
    ) {
        C created = (C) ComponentUtil.getData(vaadin, UI_COMPONENT);
        if (created != null) {
            render.bind(created, RenderItem.of(item, isSelected(item)));
        }

        ComponentUtil.setData(vaadin, ITEM, item);
        markSelected(vaadin, item);
        applyItemHeight(vaadin, item);
    }

    /**
     * The listeners of a row, attached once. A rebound row keeps them, so they read the item it stands for now.
     */
    private void listenForSelection(com.vaadin.flow.component.Component rendered) {
        rendered.getElement().addEventListener("mousedown", event -> {
        }).preventDefault();

        rendered.getElement().addEventListener("click", event -> setValue((E) ComponentUtil.getData(rendered, ITEM)));

        WebInputDetails.addClickListener(rendered.getElement(), "dblclick", details -> {
            E item = (E) ComponentUtil.getData(rendered, ITEM);
            getListenerDispatcher(ListDoubleClickEvent.class).onEvent(new ListDoubleClickEvent(this, item, details));
        });
    }

    private void markSelected(com.vaadin.flow.component.Component rendered, @Nullable E item) {
        if (isSelected(item)) {
            rendered.getElement().setAttribute("selected", true);
        }
        else {
            rendered.getElement().removeAttribute("selected");
        }
    }

    private void applyItemHeight(com.vaadin.flow.component.Component rendered, @Nullable E item) {
        rendered.getElement().getStyle()
            .set("width", "100%")
            .set("max-width", "100%")
            .set("min-width", "0")
            .set("flex", "1 1 0")
            .set("overflow", "hidden")
            .set("box-sizing", "border-box");

        if (myItemHeightGetter != null && item != null) {
            rendered.getElement().getStyle().set("height", WebLength.toCss(myItemHeightGetter.apply(item)));
        }
    }

    /**
     * The component has no rows of its own to mark, so the row a render made carries the state instead - the same
     * attribute a list box item is selected with, so one stylesheet covers both.
     */
    private com.vaadin.flow.component.Component decorate(com.vaadin.flow.component.Component rendered, @Nullable E item) {
        if (isSelected(item)) {
            rendered.getElement().setAttribute("selected", true);
        }

        if (myItemHeightGetter != null && item != null) {
            rendered.getElement().getStyle().set("height", WebLength.toCss(myItemHeightGetter.apply(item)));
        }

        // pressing on a row is what moves the focus onto the popup it is in, and a list of the platform is driven
        // from somewhere else - the lookup from the editor's caret. cancelling the press leaves the focus where it
        // was, and the click still arrives
        rendered.getElement().addEventListener("mousedown", event -> {
        }).preventDefault();

        rendered.getElement().addEventListener("click", event -> setValue(item));

        WebInputDetails.addClickListener(
            rendered.getElement(),
            "dblclick",
            details -> getListenerDispatcher(ListDoubleClickEvent.class).onEvent(new ListDoubleClickEvent(this, item, details))
        );

        return rendered;
    }

    private boolean isSelected(@Nullable E item) {
        return item != null && item.equals(myValue);
    }

    @Override
    public @Nullable E getValue() {
        return myValue;
    }

    @Override
    public void setValue(@Nullable E value, boolean fireListeners) {
        if (myValue == value) {
            return;
        }

        E previous = myValue;
        myValue = value;

        // the mark lives on the rows, and exactly two of them change - the one losing it and the one taking it.
        // rebuilding the whole window instead is a row of components per visible item over the wire, which is what
        // an arrow key and a typed character were each paying twice
        refreshRow(previous);
        refreshRow(value);

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
        }
    }

    @Override
    @RequiredUIAccess
    public void setValueByIndex(int index) {
        if (index >= 0 && index < myModel.getSize()) {
            setValue(myModel.get(index));
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
        mySpeedSearchConverter = converter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
        myItemHeightGetter = getter;

        refreshItems();
    }

    /**
     * The rows past this one are reached by scrolling, which is the whole point of the component - only a height is
     * set, and the scroll layout it sits in does the scrolling.
     */
    @Override
    public void setVisibleRowCount(int count) {
        Vaadin list = toVaadinComponent();

        if (count <= 0) {
            list.getElement().getStyle().remove("max-height");
            return;
        }

        String rowHeight = myItemHeightGetter != null && myModel.getSize() > 0
            ? WebLength.toCss(myItemHeightGetter.apply(myModel.get(0)))
            : "var(--consulo-list-row-height, 24px)";

        list.getElement().getStyle().set("max-height", "calc(" + count + " * " + rowHeight + ")");
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

    @Override
    public void setPlaceholder(LocalizeValue text) {
    }
}
