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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.Length;
import consulo.ui.ComponentItemRender;
import consulo.localize.LocalizeValue;
import consulo.ui.ListBox;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.TransferHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import io.qt.core.QMargins;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QFrame;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QListWidget;
import io.qt.widgets.QListWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtListBoxImpl<E> extends QtComponentDelegate<QListWidget> implements ListBox<E>, DesktopQtIconOwner {
    /**
     * A list of a popup is as tall as it needs to be, which is only bearable up to a point - the awt popup stops
     * at fifteen rows and scrolls the rest.
     */
    private static final int ourMaxVisibleRows = 15;

    private static final int ourMaxWidth = 800;

    private static final int ourSeparatorHeight = 7;

    /**
     * A {@link QListWidget} answers a constant hint of its own - it is built to be given a size rather than to ask
     * for one - so a popup sized from it came out the same box whatever it held.
     */
    private class QtListBox extends QListWidget {
        QtListBox(QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            super.paintEvent(event);

            String placeholder = myPlaceholder.get();
            if (count() != 0 || placeholder.isEmpty()) {
                return;
            }

            QPainter painter = new QPainter(viewport());
            try {
                painter.setPen(palette().color(QPalette.ColorRole.PlaceholderText));
                painter.drawText(viewport().rect(), Qt.AlignmentFlag.AlignCenter.value(), placeholder);
            }
            finally {
                painter.end();
            }
        }

        @Override
        public QSize sizeHint() {
            int rows = count();
            if (rows == 0) {
                return super.sizeHint();
            }

            int width = sizeHintForColumn(0);
            int height = 0;

            int visible = Math.min(rows, ourMaxVisibleRows);
            for (int i = 0; i < visible; i++) {
                height += sizeHintForRow(i);
            }

            if (visible < rows) {
                width += verticalScrollBar().sizeHint().width();
            }

            QMargins margins = contentsMargins();
            int frame = frameWidth() * 2;

            return new QSize(
                Math.min(width + margins.left() + margins.right() + frame, ourMaxWidth),
                height + margins.top() + margins.bottom() + frame
            );
        }

        /**
         * {@code itemClicked} is emitted from inside the release and carries only the row, so the event which
         * drove it is held for as long as the signal it raises is being answered.
         */
        @Override
        protected void mouseReleaseEvent(QMouseEvent event) {
            myClickEvent = event;
            try {
                super.mouseReleaseEvent(event);
            }
            finally {
                myClickEvent = null;
            }
        }

        @Override
        protected void keyPressEvent(QKeyEvent event) {
            int key = event.key();

            if (key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value()) {
                fireClicked(currentRow());
                return;
            }

            super.keyPressEvent(event);
        }
    }

    private final FlatDataModel<E> myModel;

    private TextItemRender<E> myRenderer = TextItemRender.defaultRender();

    private @Nullable ComponentItemRender<E> myComponentRender;

    private Predicate<E> mySeparatorPredicate = item -> false;

    private @Nullable Function<E, Length> myItemHeightGetter;
    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private @Nullable TransferHandler<E> myTransferHandler;
    private @Nullable Function<E, String> mySpeedSearchConverter;

    private boolean mySelectOnHover;

    private int mySelectedIndex = -1;

    // clearing the widget takes the selection down to nothing on its way, which is not a value the caller chose
    private boolean myRebuilding;

    private @Nullable QMouseEvent myClickEvent;

    public DesktopQtListBoxImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    @Override
    protected QListWidget createQt(QWidget parent) {
        return new QtListBox(parent);
    }

    @Override
    protected void initialize(QListWidget component) {
        super.initialize(component);

        component.setSelectionMode(QAbstractItemView.SelectionMode.SingleSelection);
        component.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);

        // itemEntered is only sent while the viewport is tracking the pointer, and a list does not track by default
        component.setMouseTracking(true);
        component.viewport().setMouseTracking(true);

        component.itemSelectionChanged.connect(this::onSelectionChanged);
        component.itemClicked.connect(item -> fireClicked(component.row(item)));
        component.itemDoubleClicked.connect(item -> fireDoubleClicked(component.row(item)));
        component.itemEntered.connect(item -> {
            if (mySelectOnHover) {
                component.setCurrentRow(component.row(item));
            }
        });

        rebuild(component);
    }

    /** the icon of a row is drawn from what the render answered, so the rows are asked to render once more */
    @Override
    public void refreshIcons() {
        if (myComponent == null) {
            return;
        }

        rebuild(myComponent);
    }

    private void rebuild(QListWidget component) {
        myRebuilding = true;
        try {
            rebuildItems(component);

            applySelectedIndex(component);
        }
        finally {
            myRebuilding = false;
        }
    }

    private void applySelectedIndex(QListWidget component) {
        int index = mySelectedIndex;

        // a list of a popup always offers a row - the awt popups open with the first one under the selection, and
        // a list which offers none answers the return key with nothing at all
        if (index < 0 || index >= component.count()) {
            index = mySelectOnHover ? firstSelectableRow() : -1;
        }

        if (index < 0) {
            return;
        }

        mySelectedIndex = index;

        component.setCurrentRow(index);
    }

    private int firstSelectableRow() {
        for (int i = 0; i < myModel.getSize(); i++) {
            if (!mySeparatorPredicate.test(myModel.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private void rebuildItems(QListWidget component) {
        component.clear();

        for (int i = 0; i < myModel.getSize(); i++) {
            E element = myModel.get(i);

            if (mySeparatorPredicate.test(element)) {
                addSeparatorItem(component);
                continue;
            }

            if (myComponentRender != null) {
                addRenderedItem(component, element, i);
                continue;
            }

            component.addItem(createItem(element, i));
        }
    }

    private void addSeparatorItem(QListWidget component) {
        QListWidgetItem item = new QListWidgetItem();
        item.setFlags(Qt.ItemFlag.NoItemFlags);
        // a hint of a negative width is not a valid size and qt drops it whole, height and all
        item.setSizeHint(new QSize(0, ourSeparatorHeight));

        component.addItem(item);

        // the row widget only exists once the row does, so it cannot be handed to the item before it is added
        QFrame line = new QFrame(component);
        line.setFrameShape(QFrame.Shape.HLine);
        line.setFrameShadow(QFrame.Shadow.Sunken);

        component.setItemWidget(item, line);
    }

    /**
     * A row drawn by a component of its own rather than by a text and an icon - what the plugin list is built of.
     * The component is asked for one row at a time and each answer is a component of its own, so unlike the reuse
     * the api allows for it cannot be bound again for the next row.
     */
    private void addRenderedItem(QListWidget component, E element, int index) {
        ComponentItemRender<E> render = myComponentRender;
        if (render == null) {
            return;
        }

        consulo.ui.Component rendered = render.render(RenderItem.of(element, index == mySelectedIndex));
        if (!(rendered instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        QListWidgetItem item = new QListWidgetItem();

        component.addItem(item);

        delegate.setParent(this);
        delegate.bind(component, null);

        QWidget widget = delegate.toQtComponent();
        if (widget == null) {
            return;
        }

        Function<E, Length> heightGetter = myItemHeightGetter;
        int height = heightGetter != null ? DesktopQtLength.toPixels(widget, heightGetter.apply(element)) : widget.sizeHint().height();
        item.setSizeHint(new QSize(0, height));

        component.setItemWidget(item, widget);
    }

    private QListWidgetItem createItem(E element, int index) {
        DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();

        myRenderer.render(presentation, RenderItem.of(element, index == mySelectedIndex));

        QListWidgetItem item = new QListWidgetItem(presentation.toString());

        Image image = presentation.getImage();
        if (image instanceof DesktopQtImage qtImage) {
            item.setIcon(qtImage.toQIcon());
        }

        Function<E, Length> heightGetter = myItemHeightGetter;
        if (heightGetter != null) {
            item.setSizeHint(new QSize(0, DesktopQtLength.toPixels(toQtComponent(), heightGetter.apply(element))));
        }

        return item;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onSelectionChanged() {
        QListWidget component = myComponent;
        if (myRebuilding || component == null) {
            return;
        }

        List<QListWidgetItem> selected = component.selectedItems();

        mySelectedIndex = selected.isEmpty() ? -1 : component.row(selected.get(0));

        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopQtCurrentInput.current(component)));
    }

    private void fireClicked(int row) {
        E value = valueAt(row);
        if (value == null) {
            return;
        }

        mySelectedIndex = row;

        QMouseEvent clickEvent = myClickEvent;

        InputDetails inputDetails = clickEvent != null
            ? DesktopQtInputDetails.mouse(myComponent, clickEvent)
            : DesktopQtInputDetails.mouseAtCursor(myComponent);

        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, inputDetails));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireDoubleClicked(int row) {
        E value = valueAt(row);
        if (value == null) {
            return;
        }

        mySelectedIndex = row;

        getListenerDispatcher(ListDoubleClickEvent.class)
            .onEvent(new ListDoubleClickEvent(this, value, DesktopQtCurrentInput.current(myComponent)));
    }

    /**
     * The value of a row, or {@code null} when the row stands between the others rather than being one.
     */
    private @Nullable E valueAt(int row) {
        if (row < 0 || row >= myModel.getSize()) {
            return null;
        }

        E element = myModel.get(row);

        return mySeparatorPredicate.test(element) ? null : element;
    }

    private void rebuildIfBound() {
        if (myComponent != null) {
            rebuild(myComponent);
        }
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myRenderer = render;

        rebuildIfBound();
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        myComponentRender = render;

        rebuildIfBound();
    }

    @Override
    public void isSeparator(Predicate<E> predicate) {
        mySeparatorPredicate = predicate;

        rebuildIfBound();
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;

        QListWidget widget = toQtComponent();
        if (widget != null) {
            widget.viewport().update();
        }
    }

    @Override
    @RequiredUIAccess
    public void setSelectOnHover(boolean selectOnHover) {
        mySelectOnHover = selectOnHover;
    }

    @Override
    public void setValueByIndex(int index) {
        mySelectedIndex = index;

        if (myComponent != null) {
            myComponent.setCurrentRow(index);
        }
    }

    @Override
    public @Nullable E getValue() {
        return valueAt(mySelectedIndex);
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable E value, boolean fireListeners) {
        int index = value == null ? -1 : myModel.indexOf(value);

        if (fireListeners) {
            setValueByIndex(index);
            return;
        }

        myRebuilding = true;
        try {
            setValueByIndex(index);
        }
        finally {
            myRebuilding = false;
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

        rebuildIfBound();
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
