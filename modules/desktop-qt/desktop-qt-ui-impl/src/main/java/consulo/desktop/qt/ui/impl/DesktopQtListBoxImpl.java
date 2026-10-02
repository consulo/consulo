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
import consulo.ui.ReusableComponentItemRender;
import consulo.ui.TextItemRender;
import consulo.ui.TransferHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import io.qt.core.QMargins;
import io.qt.core.QModelIndex;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QFrame;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QListWidget;
import io.qt.widgets.QListWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final int ourMaxMeasuredRows = 100;

    private static final class PooledRow {
        private final consulo.ui.Component myRendered;
        private final QWidget myWidget;
        private @Nullable Object myElement;
        private boolean mySelected;
        private boolean myDirty = true;

        private PooledRow(consulo.ui.Component rendered, QWidget widget) {
            myRendered = rendered;
            myWidget = widget;
        }
    }

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

            if (count() != 0 || myPlaceholder.isEmpty()) {
                return;
            }

            QPainter painter = new QPainter(viewport());
            try {
                painter.setPen(palette().color(QPalette.ColorRole.PlaceholderText));
                painter.drawText(viewport().rect(), Qt.AlignmentFlag.AlignCenter.value(), myPlaceholder.get());
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

            int width = isPooled() ? pooledWidthHint(this) : sizeHintForColumn(0);
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
        public void doItemsLayout() {
            super.doItemsLayout();

            layoutPool();
        }

        @Override
        protected void scrollContentsBy(int dx, int dy) {
            super.scrollContentsBy(dx, dy);

            layoutPool();
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);

            layoutPool();
        }

        @Override
        protected void keyPressEvent(QKeyEvent event) {
            int key = event.key();

            if (key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value()) {
                fireClicked(currentRow(), event);
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

    private final List<PooledRow> myPool = new ArrayList<>();

    private @Nullable PooledRow myMeasureRow;

    private boolean myPoolUnsupported;

    private boolean myItemsPooled;

    private boolean myLayingOutPool;

    private int myPooledRowHeight = -1;

    private int myPooledWidthHint = -1;

    private Qt.@Nullable ItemFlags myDefaultItemFlags;

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

        destroyPool();
        myItemsPooled = false;

        component.currentRowChanged.connect(row -> layoutPool());

        myModel.addListener(this::onModelChanged);

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

    protected void rebuild(QListWidget component) {
        myPooledWidthHint = -1;

        myRebuilding = true;
        try {
            if (isPooled()) {
                syncPooledItems(component);

                if (mySelectedIndex < 0 || mySelectedIndex >= component.count()) {
                    component.clearSelection();
                    component.setCurrentRow(-1);
                }
            }

            if (!isPooled()) {
                destroyPool();

                rebuildItems(component);
            }

            applySelectedIndex(component);

            if (myItemsPooled) {
                QListWidgetItem current = component.currentItem();
                if (current != null) {
                    component.scrollToItem(current);
                }
                else {
                    component.scrollToTop();
                }
            }

            for (PooledRow row : myPool) {
                row.myDirty = true;
            }
        }
        finally {
            myRebuilding = false;
        }

        layoutPool();
    }

    protected void rowsChanged() {
    }

    private void onModelChanged(FlatDataModelEvent event) {
        QListWidget component = myComponent;
        if (component == null) {
            return;
        }

        if (!isPooled() || !myItemsPooled || event.getType() == FlatDataModelEvent.Type.RESET) {
            rebuild(component);
            return;
        }

        int from = event.getFromIndex();
        int to = event.getToIndex();
        int span = to - from + 1;
        int count = component.count();
        int size = myModel.getSize();

        boolean consistent = from >= 0 && span > 0 && switch (event.getType()) {
            case ADDED -> count + span == size && from <= count;
            case REMOVED -> count - span == size && to < count;
            case UPDATED -> count == size && to < count;
            case RESET -> false;
        };

        if (!consistent) {
            rebuild(component);
            return;
        }

        myPooledWidthHint = -1;

        rowsChanged();

        myRebuilding = true;
        try {
            switch (event.getType()) {
                case ADDED -> {
                    for (int i = from; i <= to; i++) {
                        QListWidgetItem item = new QListWidgetItem();
                        component.insertItem(i, item);
                        configurePooledItem(component, item, myModel.get(i));
                    }
                }
                case REMOVED -> {
                    for (int i = to; i >= from; i--) {
                        component.takeItem(i);
                    }
                }
                case UPDATED -> {
                    Set<Object> updated = Collections.newSetFromMap(new IdentityHashMap<>());
                    for (int i = from; i <= to; i++) {
                        E element = myModel.get(i);
                        updated.add(element);
                        configurePooledItem(component, component.item(i), element);
                    }

                    for (PooledRow row : myPool) {
                        if (row.myElement != null && updated.contains(row.myElement)) {
                            row.myDirty = true;
                        }
                    }
                }
                default -> {
                }
            }

            int current = component.currentRow();
            if (current >= 0) {
                mySelectedIndex = current;
            }
            else {
                applySelectedIndex(component);
            }
        }
        finally {
            myRebuilding = false;
        }

        layoutPool();
    }

    private boolean isPooled() {
        return myComponentRender instanceof ReusableComponentItemRender<E, ?> && !myPoolUnsupported;
    }

    private void syncPooledItems(QListWidget component) {
        if (!myItemsPooled) {
            component.clear();
            myItemsPooled = true;
        }

        int size = myModel.getSize();
        while (component.count() > size) {
            component.takeItem(component.count() - 1);
        }

        for (int i = 0; i < size; i++) {
            QListWidgetItem item = component.item(i);
            if (item == null) {
                item = new QListWidgetItem();
                component.addItem(item);
            }

            configurePooledItem(component, item, myModel.get(i));
        }
    }

    private void configurePooledItem(QListWidget component, QListWidgetItem item, E element) {
        boolean separator = mySeparatorPredicate.test(element);
        boolean hasSeparatorLine = component.itemWidget(item) != null;

        if (separator) {
            setItemFlags(item, new Qt.ItemFlags(0));
            setItemHeight(item, ourSeparatorHeight);

            if (!hasSeparatorLine) {
                component.setItemWidget(item, createSeparatorLine(component));
            }
            return;
        }

        if (hasSeparatorLine) {
            component.removeItemWidget(item);
        }

        setItemFlags(item, defaultItemFlags());
        setItemHeight(item, pooledRowHeight(component, element));
    }

    private Qt.ItemFlags defaultItemFlags() {
        Qt.ItemFlags flags = myDefaultItemFlags;
        if (flags == null) {
            flags = new QListWidgetItem().flags();
            myDefaultItemFlags = flags;
        }
        return flags;
    }

    private static void setItemFlags(QListWidgetItem item, Qt.ItemFlags flags) {
        if (item.flags().value() != flags.value()) {
            item.setFlags(flags);
        }
    }

    private static void setItemHeight(QListWidgetItem item, int height) {
        QSize hint = item.sizeHint();
        if (hint.width() != 0 || hint.height() != height) {
            item.setSizeHint(new QSize(0, height));
        }
    }

    private int pooledRowHeight(QListWidget component, E element) {
        Function<E, Length> heightGetter = myItemHeightGetter;
        if (heightGetter != null) {
            return DesktopQtLength.toPixels(component, heightGetter.apply(element));
        }

        if (myPooledRowHeight > 0) {
            return myPooledRowHeight;
        }

        int height = 0;
        PooledRow measure = measureRow(component);
        if (measure != null) {
            bindPooledRow(measure, element, false);
            measure.myWidget.ensurePolished();
            height = measure.myWidget.sizeHint().height();
        }

        if (height <= 0) {
            return component.fontMetrics().height() + 4;
        }

        myPooledRowHeight = height;
        return height;
    }

    private int pooledWidthHint(QListWidget component) {
        if (myPooledWidthHint >= 0) {
            return myPooledWidthHint;
        }

        int width = 0;
        PooledRow measure = measureRow(component);
        if (measure != null) {
            int limit = Math.min(myModel.getSize(), ourMaxMeasuredRows);
            for (int i = 0; i < limit; i++) {
                E element = myModel.get(i);
                if (mySeparatorPredicate.test(element)) {
                    continue;
                }

                bindPooledRow(measure, element, false);
                measure.myWidget.ensurePolished();
                width = Math.max(width, measure.myWidget.sizeHint().width());
            }
        }

        myPooledWidthHint = width;
        return width;
    }

    private @Nullable PooledRow measureRow(QListWidget component) {
        PooledRow measure = myMeasureRow;
        if (measure == null) {
            measure = createPooledRow(component);
            myMeasureRow = measure;
        }
        return measure;
    }

    @SuppressWarnings("unchecked")
    private @Nullable PooledRow createPooledRow(QListWidget component) {
        if (!(myComponentRender instanceof ReusableComponentItemRender<E, ?> render)) {
            return null;
        }

        consulo.ui.Component rendered = render.createComponent();
        if (!(rendered instanceof QtComponentDelegate<?> delegate)) {
            myPoolUnsupported = true;
            return null;
        }

        delegate.setParent(this);
        delegate.bind(component.viewport(), null);

        QWidget widget = delegate.toQtComponent();
        if (widget == null) {
            myPoolUnsupported = true;
            return null;
        }

        widget.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents, true);
        widget.hide();

        return new PooledRow(rendered, widget);
    }

    @SuppressWarnings("unchecked")
    private void bindPooledRow(PooledRow row, E element, boolean selected) {
        if (myComponentRender instanceof ReusableComponentItemRender<E, ?> render) {
            ((ReusableComponentItemRender<E, consulo.ui.Component>) render).bind(row.myRendered, RenderItem.of(element, selected));
        }
    }

    private void destroyPool() {
        for (PooledRow row : myPool) {
            disposePooledRow(row);
        }
        myPool.clear();

        PooledRow measure = myMeasureRow;
        if (measure != null) {
            disposePooledRow(measure);
            myMeasureRow = null;
        }

        myPooledRowHeight = -1;
        myPooledWidthHint = -1;
    }

    private void disposePooledRow(PooledRow row) {
        if (row.myRendered instanceof QtComponentDelegate<?> delegate) {
            delegate.setParent(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void layoutPool() {
        QListWidget component = myComponent;
        if (component == null || component.isDisposed() || !myItemsPooled || !isPooled() || myLayingOutPool || myRebuilding) {
            return;
        }

        int count = component.count();
        if (count != myModel.getSize()) {
            return;
        }

        myPool.removeIf(pooled -> pooled.myWidget.isDisposed());

        myLayingOutPool = true;
        try {
            QWidget viewport = component.viewport();
            int viewportWidth = viewport.width();
            int viewportHeight = viewport.height();
            int currentRow = component.currentRow();

            int first = 0;
            QModelIndex top = component.indexAt(new QPoint(0, 0));
            if (top != null && top.isValid()) {
                first = top.row();
            }

            List<Integer> rows = new ArrayList<>();
            List<QRect> rects = new ArrayList<>();
            for (int row = first; row < count; row++) {
                QRect rect = component.visualItemRect(component.item(row));
                if (rect.top() >= viewportHeight) {
                    break;
                }

                if (rect.bottom() < 0 || mySeparatorPredicate.test(myModel.get(row))) {
                    continue;
                }

                rows.add(row);
                rects.add(rect);
            }

            Map<Object, PooledRow> byElement = new IdentityHashMap<>();
            for (PooledRow pooled : myPool) {
                if (pooled.myElement != null) {
                    byElement.put(pooled.myElement, pooled);
                }
            }

            PooledRow[] chosen = new PooledRow[rows.size()];
            Set<PooledRow> used = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int i = 0; i < rows.size(); i++) {
                PooledRow pooled = byElement.get(myModel.get(rows.get(i)));
                if (pooled != null && used.add(pooled)) {
                    chosen[i] = pooled;
                }
            }

            Deque<PooledRow> free = new ArrayDeque<>();
            for (PooledRow pooled : myPool) {
                if (!used.contains(pooled)) {
                    free.add(pooled);
                }
            }

            for (int i = 0; i < rows.size(); i++) {
                if (chosen[i] != null) {
                    continue;
                }

                PooledRow pooled = free.poll();
                if (pooled == null) {
                    pooled = createPooledRow(component);
                    if (pooled == null) {
                        break;
                    }
                    myPool.add(pooled);
                }

                pooled.myDirty = true;
                chosen[i] = pooled;
            }

            for (int i = 0; i < rows.size(); i++) {
                PooledRow pooled = chosen[i];
                if (pooled == null) {
                    continue;
                }

                int row = rows.get(i);
                E element = myModel.get(row);
                boolean selected = row == currentRow;
                if (pooled.myDirty || pooled.myElement != element || pooled.mySelected != selected) {
                    bindPooledRow(pooled, element, selected);
                    pooled.myElement = element;
                    pooled.mySelected = selected;
                    pooled.myDirty = false;
                }

                QRect rect = rects.get(i);
                pooled.myWidget.setGeometry(rect.x(), rect.y(), Math.max(viewportWidth - rect.x(), rect.width()), rect.height());
                if (pooled.myWidget.isHidden()) {
                    pooled.myWidget.show();
                }
            }

            for (PooledRow pooled : free) {
                pooled.myElement = null;
                if (!pooled.myWidget.isHidden()) {
                    pooled.myWidget.hide();
                }
            }
        }
        finally {
            myLayingOutPool = false;
        }

        if (myPoolUnsupported) {
            rebuild(component);
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
        myItemsPooled = false;

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
        component.setItemWidget(item, createSeparatorLine(component));
    }

    private static QFrame createSeparatorLine(QListWidget component) {
        QFrame line = new QFrame(component);
        line.setFrameShape(QFrame.Shape.HLine);
        line.setFrameShadow(QFrame.Shadow.Sunken);
        return line;
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
        fireClicked(row, null);
    }

    private void fireClicked(int row, @Nullable QKeyEvent keyEvent) {
        E value = valueAt(row);
        if (value == null) {
            return;
        }

        mySelectedIndex = row;

        QMouseEvent clickEvent = myClickEvent;

        InputDetails inputDetails;
        if (keyEvent != null) {
            inputDetails = DesktopQtInputDetails.keyboard(myComponent, keyEvent);
        }
        else if (clickEvent != null) {
            inputDetails = DesktopQtInputDetails.mouse(myComponent, clickEvent);
        }
        else {
            inputDetails = DesktopQtInputDetails.mouseAtCursor(myComponent);
        }

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
        destroyPool();
        myPoolUnsupported = false;

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

        layoutPool();
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
        myPooledRowHeight = -1;

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
