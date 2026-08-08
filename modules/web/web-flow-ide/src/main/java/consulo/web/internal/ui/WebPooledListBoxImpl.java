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
package consulo.web.internal.ui;

import consulo.ui.TransferHandler;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import consulo.ui.ComponentItemRender;
import consulo.ui.ListBox;
import consulo.ui.RenderItem;
import consulo.ui.ReusableComponentItemRender;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import consulo.ui.model.LazyFlatDataModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.ToVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A list which scrolls over a count rather than over its rows.
 * <p/>
 * The rows are a fixed pool - as many as fit on screen and a few more - created once and rebound as the window moves
 * or the items change. Nothing is added or removed after that, so a list whose contents changed costs the properties
 * which actually differ and not a tree of components per visible line.
 * <p/>
 * This is the swing arrangement, which a browser can have but does not get for free. A {@code JList} paints its rows
 * with one shared component that the renderer reconfigures per row, so rebuilding the model is free - there is nothing
 * per row to rebuild. The dom is retained rather than painted, so the rows must exist; the nearest thing is to keep
 * them and change only what they say. Vaadin's own virtual list cannot do it, because a data reset runs
 * {@code destroyAllData}, which detaches every rendered component whatever its key.
 * <p/>
 * What keeps the scrollbar honest with no rows to measure is the count: the scroll area is
 * {@code itemCount * rowHeight}, pushed as a single number, so a model of five items and one of five hundred cost the
 * same.
 *
 * @author VISTALL
 */
@SuppressWarnings("unchecked")
public class WebPooledListBoxImpl<E> extends VaadinComponentDelegate<WebPooledListBoxImpl.Vaadin> implements ListBox<E> {
    private @Nullable TransferHandler<E> myTransferHandler;
    /**
     * Rows kept beyond the viewport on each side, matching the overscan the script uses - the pool has to be able to
     * hold everything the script may ask for.
     */
    private static final int OVERSCAN = 4;

    private static final int DEFAULT_ROW_HEIGHT = 24;

    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips rebuilding
    // on css only changes
    // HasSize because a list is put inside a scroll layout, which sizes whatever it is given. this one scrolls
    // itself - it is the element the window is measured from - and the layout around it never overflows, because
    // the max height set here keeps the list inside it
    @Tag("consulo-virtual-list")
    @StyleSheet("/list/webListBox.css")
    public static class Vaadin extends com.vaadin.flow.component.Component
        implements FromVaadinComponentWrapper, com.vaadin.flow.component.HasSize {
        private @Nullable WebPooledListBoxImpl<?> myOwner;

        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return myOwner;
        }
    }

    /**
     * One pooled row - the component a render made, the vaadin component it is drawn with, and the item it currently
     * stands for.
     */
    private final class Row {
        private final consulo.ui.Component myRendered;
        private final com.vaadin.flow.component.Component myVaadin;

        private @Nullable E myItem;
        private int myIndex = -1;

        private Row(consulo.ui.Component rendered) {
            myRendered = rendered;
            myVaadin = ((ToVaadinComponentWrapper) rendered).toVaadinComponent();

            // read when the press fires rather than when it is attached, because the row stands for a different item
            // every time the window moves
            myVaadin.getElement().addEventListener("mousedown", event -> {
            }).preventDefault();
            myVaadin.getElement().addEventListener("click", event -> {
                if (myItem != null) {
                    setValue(myItem);
                }
            });
            myVaadin.getElement().addEventListener(
                "dblclick",
                event -> getListenerDispatcher(ListDoubleClickEvent.class)
                    .onEvent(new ListDoubleClickEvent(WebPooledListBoxImpl.this, myItem))
            );
        }
    }

    private final FlatDataModel<E> myModel;
    private final List<Row> myPool = new ArrayList<>();

    private @Nullable ReusableComponentItemRender<E, ?> myReusableRender;
    private @Nullable ComponentItemRender<E> myRender;
    private @Nullable ToIntFunction<E> myItemHeightGetter;
    private @Nullable Function<E, String> mySpeedSearchConverter;

    private @Nullable E myValue;
    private int myFirst;
    private int myCount;
    private int myVisibleRowCount;

    public WebPooledListBoxImpl(FlatDataModel<E> model) {
        myModel = model;

        Vaadin vaadin = toVaadinComponent();
        vaadin.myOwner = this;
        vaadin.addClassName("web-list-box");

        vaadin.getElement().addEventListener("consulo-range", event -> {
            int first = event.getEventData().path("event.detail.first").asInt(0);
            int count = event.getEventData().path("event.detail.count").asInt(0);

            bindRange(first, count);
        })
            .addEventData("event.detail.first")
            .addEventData("event.detail.count");

        model.addListener(this::onModelChanged);

        // by hand, and on every attach - flow drops @JavaScript pointing at a context absolute path, and a reloaded
        // page is a dom which has none of the definition the old one was given
        vaadin.getElement().addAttachListener(event -> loadScript());
        loadScript();

        pushItemCount();
    }

    /**
     * Puts the element definition in the page, once. Everything pushed at the element before it lands is a call on a
     * plain unknown element and does nothing, so the count and the rows are sent again once it is defined.
     */
    private void loadScript() {
        toVaadinComponent().getElement().executeJs("""
            if (window.customElements.get('consulo-virtual-list')) {
                return;
            }

            let script = document.getElementById('consulo-virtual-list-script');
            if (!script) {
                script = document.createElement('script');
                script.id = 'consulo-virtual-list-script';
                script.src = '/list/consuloVirtualList.js';
                document.head.appendChild(script);
            }
            """);
    }

    // ------------------------------------------------------------------------------------------------
    // the pool
    // ------------------------------------------------------------------------------------------------

    /**
     * Binds the rows the script says are on screen. The pool grows to whatever the widest window needed and never
     * shrinks - a row taken off screen is rebound rather than thrown away.
     */
    @RequiredUIAccess
    private void bindRange(int first, int count) {
        myFirst = first;
        myCount = count;

        while (myPool.size() < count) {
            Row row = createRow();
            if (row == null) {
                break;
            }
            myPool.add(row);
            toVaadinComponent().getElement().appendChild(row.myVaadin.getElement());
        }

        for (int i = 0; i < myPool.size(); i++) {
            Row row = myPool.get(i);

            int index = first + i;
            if (i >= count || index < 0 || index >= myModel.getSize()) {
                // beyond the model - kept, and moved out of sight rather than removed
                row.myItem = null;
                row.myIndex = -1;
                row.myVaadin.getElement().getStyle().set("display", "none");
                continue;
            }

            row.myVaadin.getElement().getStyle().remove("display");
            bindRow(row, myModel.get(index), index);
        }
    }

    private @Nullable Row createRow() {
        if (myReusableRender != null) {
            return new Row(myReusableRender.createComponent());
        }
        return null;
    }

    @RequiredUIAccess
    private void bindRow(Row row, E item, int index) {
        row.myItem = item;
        row.myIndex = index;

        if (myReusableRender != null) {
            ((ReusableComponentItemRender<E, consulo.ui.Component>)myReusableRender)
                .bind(row.myRendered, RenderItem.of(item, item.equals(myValue)));
        }

        if (item.equals(myValue)) {
            row.myVaadin.getElement().setAttribute("selected", true);
        }
        else {
            row.myVaadin.getElement().removeAttribute("selected");
        }

        toVaadinComponent().getElement()
            .executeJs(
                "window.customElements.whenDefined('consulo-virtual-list').then(() => this.placeRow($0, $1));",
                row.myVaadin.getElement(),
                index
            );
    }

    /**
     * Rebinds whatever is on screen right now, without asking the browser where that is - the server already knows,
     * and a round trip to be told again is the thing this list exists to avoid.
     */
    @RequiredUIAccess
    private void rebindCurrent() {
        bindRange(myFirst, Math.max(myCount, myPool.size()));
    }

    @RequiredUIAccess
    private void onModelChanged(FlatDataModelEvent event) {
        pushItemCount();
        rebindCurrent();
    }

    /**
     * The only thing a change of length costs. Everything the scrollbar needs follows from it.
     */
    private void pushItemCount() {
        int count = myModel.getSize();
        int rowHeight = rowHeight();

        // the height is set here rather than left to the content. the scroll layout around the list calls
        // setSizeFull, which is height 100% against parents that have no height of their own - so the intrinsic
        // height the scroll area would have given collapsed to nothing and the list was 0px tall. both numbers are
        // known on this side, so it is said outright
        int visible = myVisibleRowCount > 0 ? Math.min(count, myVisibleRowCount) : count;

        toVaadinComponent().getElement().executeJs(
            // waited on rather than guarded - a push dropped because the definition had not landed yet never came
            // back, and the list stayed on whatever it was built with
            """
            window.customElements.whenDefined('consulo-virtual-list').then(() => {
                this.style.height = $2 + 'px';
                this.setRowHeight($1);
                this.setItemCount($0);
            });
            """,
            count,
            rowHeight,
            visible * rowHeight
        );
    }

    private int rowHeight() {
        if (myItemHeightGetter != null && myModel.getSize() > 0) {
            return myItemHeightGetter.applyAsInt(myModel.get(0));
        }
        return DEFAULT_ROW_HEIGHT;
    }

    // ------------------------------------------------------------------------------------------------
    // ListBox
    // ------------------------------------------------------------------------------------------------

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    /**
     * A text render is turned into a component one, because the pool keeps rows and a presentation is built per call
     * with no identity to rebind. The kept row is a holder whose content is replaced - the runs a presentation makes
     * are cheap next to the row itself, which is what the pool is protecting.
     */
    @Override
    public void setRender(TextItemRender<E> render) {
        setRender(ComponentItemRender.<E, TextRow>reusable(
            TextRow::new,
            (row, item) -> {
                WebItemPresentationImpl presentation = new WebItemPresentationImpl();
                render.render(presentation, item);
                row.setContent(presentation.toComponent());
            }
        ));
    }

    @Tag("div")
    public static class TextRowVaadin extends com.vaadin.flow.component.Component implements FromVaadinComponentWrapper {
        private @Nullable TextRow myOwner;

        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return myOwner;
        }
    }

    /**
     * Holds whatever a text render made, so the pool has one component per row rather than one per call.
     */
    public static class TextRow extends VaadinComponentDelegate<TextRowVaadin> {
        public TextRow() {
            toVaadinComponent().myOwner = this;
        }

        private void setContent(com.vaadin.flow.component.Component content) {
            com.vaadin.flow.dom.Element element = toVaadinComponent().getElement();

            element.removeAllChildren();
            element.appendChild(content.getElement());
        }

        @Override
        public TextRowVaadin createVaadinComponent() {
            return new TextRowVaadin();
        }
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        myRender = render;

        if (render instanceof ReusableComponentItemRender<E, ?> reusable) {
            myReusableRender = reusable;
        }

        // the pool was made by the render that came before, so it goes and is built again on the next range
        for (Row row : myPool) {
            row.myVaadin.getElement().removeFromParent();
        }
        myPool.clear();

        rebindCurrent();
    }

    @Override
    public @Nullable E getValue() {
        return myValue;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable E value, boolean fireListeners) {
        if (myValue == value) {
            return;
        }

        E previous = myValue;
        myValue = value;

        // exactly two rows change - the one losing the mark and the one taking it
        for (Row row : myPool) {
            if (row.myItem != null && (row.myItem.equals(previous) || row.myItem.equals(value))) {
                bindRow(row, row.myItem, row.myIndex);
            }
        }

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
    public void setItemHeightGetter(@Nullable ToIntFunction<E> getter) {
        myItemHeightGetter = getter;
        pushItemCount();
    }

    /**
     * How tall the list is allowed to grow. What scrolls the rest past it is this element itself - unlike the lists
     * built on vaadin's own, this one owns its scrolling, because the window it reports is measured from it.
     */
    @Override
    public void setVisibleRowCount(int count) {
        myVisibleRowCount = count;

        // the height itself follows from the count and the model, and is set with them
        pushItemCount();
    }

    /**
     * Brings a row into view. The arithmetic is the browser's - it is the one that knows where it is scrolled to.
     */
    @RequiredUIAccess
    public void scrollToIndex(int index) {
        toVaadinComponent().getElement().executeJs("window.customElements.whenDefined('consulo-virtual-list').then(() => this.scrollToIndex($0));", index);
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
