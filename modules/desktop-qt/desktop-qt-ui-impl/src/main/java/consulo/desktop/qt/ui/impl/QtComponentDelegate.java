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
import consulo.desktop.qt.ui.impl.image.DesktopQtIconRefresher;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.BorderBuilder;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.HasSize;
import consulo.ui.Length;
import consulo.ui.PaddingBuilder;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.cursor.Cursor;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.event.AttachEvent;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.DetachEvent;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.event.KeyReleasedEvent;
import consulo.ui.impl.BorderBuilderImpl;
import consulo.ui.impl.PaddingBuilderImpl;
import consulo.ui.impl.UIDataObject;
import consulo.ui.internal.BorderPosition;
import consulo.util.dataholder.Key;
import io.qt.core.QEvent;
import io.qt.core.QMargins;
import io.qt.core.QObject;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QCursor;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public abstract class QtComponentDelegate<T extends QWidget> implements Component, HasSize, HasFocus {
    private static final int ourUnsetSize = -1;

    private int myWidth = ourUnsetSize;
    private int myHeight = ourUnsetSize;
    private int myMinWidth = ourUnsetSize;
    private int myMinHeight = ourUnsetSize;

    private @Nullable List<Consumer<QWidget>> myBindListeners;

    /** the two blocks of the style sheet of the widget, so either of them can be rewritten on its own */
    private String myOwnStyleSheet = "";
    private static final int HAIRLINE = 1;
    private static final int ARC = 4;

    private final Map<BorderPosition, Integer> myExtraPadding = new EnumMap<>(BorderPosition.class);


    private String myBorderStyleSheet = "";

    protected UIDataObject myDataObject = new UIDataObject();

    protected T myComponent;

    private Component myParent;

    private boolean myKeyDispatchInstalled;
    private boolean myAttachDispatchInstalled;
    private boolean myClickDispatchInstalled;
    private boolean myContextMenuDispatchInstalled;

    private boolean myEnabled = true;
    private boolean myVisible = true;

    private LocalizeValue myToolTipText = LocalizeValue.empty();
    private @Nullable ColorValue myForegroundColor;
    private @Nullable ColorValue myBackgroundColor;
    private @Nullable Cursor myCursor;

    /** the desktop frontend draws into a single ui, so every component of it answers the same access */
    @Override
    public UIAccess getUIAccess() {
        return DesktopQtUIAccess.INSTANCE;
    }

    public final void bind(QWidget parent, @Nullable Object layoutData) {
        if (myComponent != null) {
            if (!myComponent.isDisposed()) {
                return;
            }

            // qt took the widget down along with the tree it stood in - a dialog which was closed - without this
            // component being told. The java object outlived the native one, so what is held is a widget every
            // call on which throws, and the component has to be given a new one instead of handing that one out
            myComponent = null;
            myOwnStyleSheet = "";
            myBorderStyleSheet = "";
        }

        myComponent = createQt(parent);

        // qt destroys a widget along with the parent it was given to, and the component is not told. Everything
        // here asks whether it has a widget by whether this field is set, so the field is cleared the moment the
        // widget behind it goes - otherwise what is held is a widget every call on which throws
        T bound = myComponent;
        bound.destroyed.connect(() -> {
            if (myComponent == bound) {
                myComponent = null;
            }
        });

        TargetQt.register(myComponent, this);

        myComponent.setEnabled(myEnabled);

        // a widget without a parent turns into a top level window the moment it is shown, and a child is
        // shown by its parent anyway - so only an explicit hide is worth applying here
        if (!myVisible) {
            myComponent.setVisible(false);
        }

        applySize();

        initialize(myComponent);

        if (this instanceof DesktopQtIconOwner iconOwner) {
            DesktopQtIconRefresher.register(iconOwner);
        }

        // after the component styled itself, so that a border is added to that style sheet rather than replacing it
        applyBorders();

        List<Consumer<QWidget>> bindListeners = myBindListeners;
        if (bindListeners != null) {
            for (Consumer<QWidget> bindListener : bindListeners) {
                bindListener.accept(myComponent);
            }
        }
    }

    /**
     * The qt widget of a component only exists once the component is bound to a parent, and whoever needs the
     * widget - a context menu installed on it - may well arrive before that.
     * <p/>
     * A component is bound more than once: hiding a tool window disposes the widget of everything in it and
     * showing it again builds a fresh one. What was hung off the widget belonged to the widget and went with it,
     * so every listener is kept and run again against the new one rather than dropped after the first bind.
     */
    public void whenBound(Consumer<QWidget> consumer) {
        if (myBindListeners == null) {
            myBindListeners = new ArrayList<>();
        }

        myBindListeners.add(consumer);

        if (myComponent != null) {
            consumer.accept(myComponent);
        }
    }

    public @Nullable T toQtComponent() {
        return myComponent;
    }

    protected abstract T createQt(QWidget parent);

    protected void initialize(T component) {
    }

    public void setParent(@Nullable Component component) {
        myParent = component;

        if (component == null) {
            disposeQt();
        }
    }

    public void disposeQt() {
        if (myComponent != null) {
            TargetQt.unregister(myComponent);

            myComponent.setParent((QWidget) null);
            myComponent.dispose();
            myComponent = null;

            myOwnStyleSheet = "";
            myBorderStyleSheet = "";
        }
    }

    @RequiredUIAccess
    @Override
    public BorderBuilder borderBuilder() {
        return new BorderBuilderImpl(myDataObject, this::applyBorders);
    }

    @RequiredUIAccess
    @Override
    public PaddingBuilder paddingBuilder() {
        return new PaddingBuilderImpl(myDataObject, this::applyBorders);
    }

    /**
     * Room this frontend measured for itself - the bar a window manager drew over the top of a window - rather
     * than a step of the scale a screen asked for.
     */
    @RequiredUIAccess
    public void addPaddingInPixels(BorderPosition borderPosition, int pixels) {
        myExtraPadding.put(borderPosition, pixels);

        applyBorders();
    }


    /**
     * The style sheet a component writes for itself - a flat button dropping its frame. It is kept apart from the
     * block the borders own so that neither of the two takes the other back out.
     */
    protected void setOwnStyleSheet(String styleSheet) {
        if (myOwnStyleSheet.equals(styleSheet)) {
            return;
        }

        myOwnStyleSheet = styleSheet;

        applyStyleSheet();
    }

    private void applyStyleSheet() {
        if (myComponent != null) {
            myComponent.setStyleSheet(myOwnStyleSheet + myBorderStyleSheet);
        }
    }

    /**
     * A border of the api is either a rule or plain space, and a qt widget answers the two in different places: a
     * style sheet draws the rule, while the contents margin of the widget is what both the layout of a container
     * and the widgets which know the box model - a label, a list - read to keep clear of it.
     */
    private void applyBorders() {
        if (myComponent == null) {
            return;
        }

        Map<BorderPosition, ColorValue> borders = myDataObject.getBorders();
        Map<BorderPosition, Space> paddings = myDataObject.getPaddings();

        int[] space = new int[BorderPosition.values().length];

        StringBuilder rules = new StringBuilder();

        for (BorderPosition position : BorderPosition.values()) {
            Space padding = paddings.get(position);
            if (padding != null) {
                space[position.ordinal()] = DesktopQtSpace.toPixels(padding);
            }

            Integer extra = myExtraPadding.get(position);
            if (extra != null) {
                space[position.ordinal()] += extra;
            }

            ColorValue colorValue = borders.get(position);
            if (colorValue == null) {
                continue;
            }

            space[position.ordinal()] += HAIRLINE;

            rules.append("border-")
                .append(toCssEdge(position))
                .append(": ")
                .append(HAIRLINE)
                .append("px solid ")
                .append(toCssColor(colorValue))
                .append("; ");
        }

        if (borders.size() == BorderPosition.values().length) {
            rules.append("border-radius: ").append(ARC).append("px; ");
        }


        // a widget keeps drawing the frame of its style until the style sheet drops it, and a single edge on its
        // own is not enough to make it do so
        if (!rules.isEmpty()) {
            rules.insert(0, "border: none; ");
        }

        String rule = rules.isEmpty() ? "" : "#" + borderObjectName() + " { " + rules + "}";

        if (!myBorderStyleSheet.equals(rule)) {
            myBorderStyleSheet = rule;

            applyStyleSheet();
        }

        QMargins margins = new QMargins(
            space[BorderPosition.LEFT.ordinal()],
            space[BorderPosition.TOP.ordinal()],
            space[BorderPosition.RIGHT.ordinal()],
            space[BorderPosition.BOTTOM.ordinal()]
        );

        if (!margins.equals(myComponent.contentsMargins())) {
            myComponent.setContentsMargins(margins);
        }
    }

    private String borderObjectName() {
        String objectName = myComponent.objectName();

        if (objectName == null || objectName.isEmpty()) {
            // a rule which does not name the widget is inherited by every child of it, and each of them would then
            // draw a border of its own
            objectName = "consuloBordered" + System.identityHashCode(myComponent);

            myComponent.setObjectName(objectName);
        }

        return objectName;
    }

    private static String toCssEdge(BorderPosition position) {
        return switch (position) {
            case TOP -> "top";
            case BOTTOM -> "bottom";
            case LEFT -> "left";
            case RIGHT -> "right";
        };
    }

    private static String toCssColor(@Nullable ColorValue colorValue) {
        if (colorValue == null) {
            return "palette(mid)";
        }

        RGBColor rgb = colorValue.toRGB();

        return "rgba(%d, %d, %d, %d%%)".formatted(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha() * 100 / 255);
    }

    @Override
    public boolean isVisible() {
        return myVisible;
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        myVisible = value;

        if (myComponent != null) {
            myComponent.setVisible(value);
        }
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        myEnabled = value;

        if (myComponent != null) {
            myComponent.setEnabled(value);
        }
    }

    @Override
    public @Nullable Component getParent() {
        return myParent;
    }

    /**
     * A dimension of the api is the size the component is meant to have, the way a css width is in the web
     * frontend - a qt widget inside a layout only keeps such a size when it is fixed, since a plain resize is
     * undone by the next pass of the layout.
     */
    @RequiredUIAccess
    public void setAccessibleName(LocalizeValue name) {
        whenBound(widget -> widget.setAccessibleName(name.get()));
    }

    @RequiredUIAccess
    public void setAccessibleDescription(LocalizeValue description) {
        whenBound(widget -> widget.setAccessibleDescription(description.get()));
    }

    private int toPixels(Length length) {
        QWidget widget = toQtComponent();
        return widget == null ? length.toPixels(16) : DesktopQtLength.toPixels(widget, length);
    }

    @RequiredUIAccess
    @Override
    public void setWidth(Length length) {
        myWidth = toPixels(length);

        applySize();
    }

    @RequiredUIAccess
    @Override
    public void setHeight(Length length) {
        myHeight = toPixels(length);

        applySize();
    }

    @RequiredUIAccess
    @Override
    public void setMinWidth(Length length) {
        myMinWidth = toPixels(length);

        applySize();
    }

    @RequiredUIAccess
    @Override
    public void setMinHeight(Length length) {
        myMinHeight = toPixels(length);

        applySize();
    }

    private void applySize() {
        if (myComponent == null) {
            return;
        }

        if (myWidth != ourUnsetSize) {
            myComponent.setFixedWidth(myWidth);
        }

        if (myHeight != ourUnsetSize) {
            myComponent.setFixedHeight(myHeight);
        }

        if (myMinWidth != ourUnsetSize) {
            myComponent.setMinimumWidth(myMinWidth);
        }

        if (myMinHeight != ourUnsetSize) {
            myComponent.setMinimumHeight(myMinHeight);
        }
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return myDataObject.getDispatcher(eventClass);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(
        Class<? extends E> eventClass,
        ComponentEventListener<C, E> listener
    ) {
        if (eventClass == KeyPressedEvent.class || eventClass == KeyReleasedEvent.class) {
            installKeyDispatch();
        }

        if (eventClass == AttachEvent.class || eventClass == DetachEvent.class) {
            installAttachDispatch();
        }

        if (eventClass == ClickEvent.class) {
            installClickDispatch();
        }

        if (eventClass == ContextMenuEvent.class) {
            installContextMenuDispatch();
        }

        return myDataObject.addListener(eventClass, listener);
    }

    /**
     * Starts passing key presses on to the api, which is what the awt delegate does for every component it makes
     * by hanging its key adapters off it.
     * <p>
     * A filter rather than an override, since the widget of each component is built by that component and they
     * cannot all be made to override {@code keyPressEvent} - and it is installed only once something has asked
     * for the keys, so widgets nobody listens to pay nothing per keystroke.
     */
    private void installKeyDispatch() {
        if (myKeyDispatchInstalled) {
            return;
        }

        myKeyDispatchInstalled = true;

        whenBound(widget -> widget.installEventFilter(new QObject(widget) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                if (event instanceof QKeyEvent keyEvent) {
                    if (event.type() == QEvent.Type.KeyPress) {
                        fireComponentEvent(KeyPressedEvent.class, new KeyPressedEvent(
                            QtComponentDelegate.this,
                            DesktopQtInputDetails.keyboard(widget, keyEvent)
                        ));
                    }
                    else if (event.type() == QEvent.Type.KeyRelease) {
                        fireComponentEvent(KeyReleasedEvent.class, new KeyReleasedEvent(
                            QtComponentDelegate.this,
                            DesktopQtInputDetails.keyboard(widget, keyEvent)
                        ));
                    }
                }

                // never swallowed here - the widget still does whatever the key normally does to it
                return false;
            }
        }));
    }

    private void installContextMenuDispatch() {
        if (myContextMenuDispatchInstalled) {
            return;
        }

        myContextMenuDispatchInstalled = true;

        whenBound(widget -> {
            widget.setContextMenuPolicy(io.qt.core.Qt.ContextMenuPolicy.DefaultContextMenu);

            widget.installEventFilter(new QObject(widget) {
                @Override
                public boolean eventFilter(QObject watched, QEvent event) {
                    if (event.type() == QEvent.Type.ContextMenu) {
                        fireComponentEvent(ContextMenuEvent.class, new ContextMenuEvent(
                            QtComponentDelegate.this,
                            DesktopQtInputDetails.mouseAtCursor(widget)
                        ));
                        return true;
                    }

                    return false;
                }
            });
        });
    }

    /**
     * Starts passing clicks on to the api. Qt hands a mouse event to the deepest widget under the pointer and only
     * passes it on when that widget lets it go, so swallowing it here is what keeps a click on something inside a
     * row from counting as a click on the row as well.
     */
    private void installClickDispatch() {
        if (myClickDispatchInstalled) {
            return;
        }

        myClickDispatchInstalled = true;

        whenBound(widget -> widget.installEventFilter(new QObject(widget) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                if (event.type() == QEvent.Type.MouseButtonRelease
                    && event instanceof QMouseEvent mouseEvent
                    && mouseEvent.button() == io.qt.core.Qt.MouseButton.LeftButton) {
                    fireComponentEvent(ClickEvent.class, new ClickEvent(
                        QtComponentDelegate.this,
                        DesktopQtInputDetails.mouse(widget, mouseEvent)
                    ));
                    return true;
                }

                return false;
            }
        }));
    }

    /**
     * Starts reporting whether the component is on screen. A widget is bound to its parent long before the window
     * holding it is shown, and what the api calls an attach is that moment of becoming visible - a toolbar hangs
     * the expansion of its action group off it, so a toolbar which is never told stays empty.
     * <p>
     * Qt shows and hides the whole tree under a window along with it, so the events of the widget itself are
     * enough and no ancestor has to be watched.
     */
    private void installAttachDispatch() {
        if (myAttachDispatchInstalled) {
            return;
        }

        myAttachDispatchInstalled = true;

        whenBound(widget -> widget.installEventFilter(new QObject(widget) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                if (event.type() == QEvent.Type.Show) {
                    fireComponentEvent(AttachEvent.class, new AttachEvent(QtComponentDelegate.this));
                }
                else if (event.type() == QEvent.Type.Hide) {
                    fireComponentEvent(DetachEvent.class, new DetachEvent(QtComponentDelegate.this));
                }

                return false;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private <E extends ComponentEvent<Component>> void fireComponentEvent(Class<E> eventClass, E event) {
        ((ComponentEventListener<Component, E>) getListenerDispatcher(eventClass)).onEvent(event);
    }

    @Override
    public <T1> @Nullable T1 getUserData(Key<T1> key) {
        return myDataObject.getUserData(key);
    }

    @Override
    public <T1> void putUserData(Key<T1> key, @Nullable T1 value) {
        myDataObject.putUserData(key, value);
    }

    @Override
    public void setToolTipText(LocalizeValue value) {
        myToolTipText = value;

        if (myComponent != null) {
            myComponent.setToolTip(value.get());
        }
    }

    @Override
    public LocalizeValue getToolTipText() {
        return myToolTipText;
    }

    @Override
    public @Nullable ColorValue getForegroundColor() {
        return myForegroundColor;
    }

    @Override
    public void setForegroundColor(@Nullable ColorValue foreground) {
        myForegroundColor = foreground;

        applyColors();
    }

    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    @Override
    public void setBackgroundColor(@Nullable ColorValue background) {
        myBackgroundColor = background;

        applyColors();
    }

    private void applyColors() {
        if (myComponent == null) {
            return;
        }

        QPalette palette = new QPalette(myComponent.palette());

        if (myForegroundColor != null) {
            palette.setColor(QPalette.ColorRole.WindowText, TargetQt.to(myForegroundColor));
            palette.setColor(QPalette.ColorRole.Text, TargetQt.to(myForegroundColor));
            palette.setColor(QPalette.ColorRole.ButtonText, TargetQt.to(myForegroundColor));
        }

        if (myBackgroundColor != null) {
            palette.setColor(QPalette.ColorRole.Window, TargetQt.to(myBackgroundColor));
            palette.setColor(QPalette.ColorRole.Base, TargetQt.to(myBackgroundColor));

            // a widget only paints its Window role when it is told to fill its own background
            myComponent.setAutoFillBackground(true);
        }

        myComponent.setPalette(palette);
    }

    @Override
    public @Nullable Cursor getCursor() {
        return myCursor;
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        myCursor = cursor;

        if (myComponent != null) {
            myComponent.setCursor(new QCursor(toCursorShape(cursor)));
        }
    }

    static Qt.CursorShape toCursorShape(@Nullable Cursor cursor) {
        if (!(cursor instanceof StandardCursors standardCursor)) {
            return Qt.CursorShape.ArrowCursor;
        }

        return switch (standardCursor) {
            case CROSSHAIR -> Qt.CursorShape.CrossCursor;
            case TEXT -> Qt.CursorShape.IBeamCursor;
            case WAIT -> Qt.CursorShape.WaitCursor;
            case HAND -> Qt.CursorShape.PointingHandCursor;
            case ARROW -> Qt.CursorShape.ArrowCursor;
        };
    }

    public void focus() {
        if (myComponent != null) {
            myComponent.setFocus();
        }
    }

    public boolean hasFocus() {
        T component = myComponent;
        if (component == null || component.isDisposed()) {
            return false;
        }

        if (component.hasFocus()) {
            return true;
        }

        // a component which is a container of others - the editor is a scroll area and a strip side by side -
        // never holds the focus itself, one of the widgets inside it does, and to everything asking whether the
        // editor has focus that is the same thing
        QWidget focused = QApplication.focusWidget();

        return focused != null && !focused.isDisposed() && component.isAncestorOf(focused);
    }

    public void setFocusable(boolean focusable) {
        if (myComponent != null) {
            myComponent.setFocusPolicy(focusable ? io.qt.core.Qt.FocusPolicy.StrongFocus : io.qt.core.Qt.FocusPolicy.NoFocus);
        }
    }

    public boolean isFocusable() {
        return myComponent == null || myComponent.focusPolicy() != io.qt.core.Qt.FocusPolicy.NoFocus;
    }
}
