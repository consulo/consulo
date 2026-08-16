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
import consulo.ui.Component;
import consulo.ui.HasSize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.cursor.Cursor;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.impl.BorderInfo;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import io.qt.core.QMargins;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QCursor;
import io.qt.gui.QPalette;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public abstract class QtComponentDelegate<T extends QWidget> implements Component, HasSize {
    private static final int ourUnsetSize = -1;

    private int myWidth = ourUnsetSize;
    private int myHeight = ourUnsetSize;
    private int myMinWidth = ourUnsetSize;
    private int myMinHeight = ourUnsetSize;

    private @Nullable List<Consumer<QWidget>> myBindListeners;

    /** the two blocks of the style sheet of the widget, so either of them can be rewritten on its own */
    private String myOwnStyleSheet = "";
    private String myBorderStyleSheet = "";

    protected UIDataObject myDataObject = new UIDataObject();

    protected T myComponent;

    private Component myParent;

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
            return;
        }

        myComponent = createQt(parent);

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
            myBindListeners = null;

            for (Consumer<QWidget> bindListener : bindListeners) {
                bindListener.accept(myComponent);
            }
        }
    }

    /**
     * The qt widget of a component only exists once the component is bound to a parent, and whoever needs the
     * widget - a context menu installed on it - may well arrive before that.
     */
    public void whenBound(Consumer<QWidget> consumer) {
        if (myComponent != null) {
            consumer.accept(myComponent);
            return;
        }

        if (myBindListeners == null) {
            myBindListeners = new ArrayList<>();
        }

        myBindListeners.add(consumer);
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
    public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, @Nullable ColorValue colorValue, int width) {
        myDataObject.addBorder(borderPosition, borderStyle, colorValue, width);

        applyBorders();
    }

    @RequiredUIAccess
    @Override
    public void removeBorder(BorderPosition borderPosition) {
        myDataObject.removeBorder(borderPosition);

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

        Map<BorderPosition, BorderInfo> borders = myDataObject.getBorders();

        int[] space = new int[BorderPosition.values().length];

        StringBuilder rules = new StringBuilder();

        for (BorderPosition position : BorderPosition.values()) {
            BorderInfo info = borders.get(position);
            if (info == null) {
                continue;
            }

            space[position.ordinal()] = info.getWidth();

            if (info.getBorderStyle() == BorderStyle.LINE) {
                rules.append("border-")
                    .append(toCssEdge(position))
                    .append(": ")
                    .append(info.getWidth())
                    .append("px solid ")
                    .append(toCssColor(info.getColorValue()))
                    .append("; ");
            }
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
    @Override
    public void setWidth(int widthInPixels) {
        myWidth = widthInPixels;

        applySize();
    }

    @RequiredUIAccess
    @Override
    public void setHeight(int heightInPixels) {
        myHeight = heightInPixels;

        applySize();
    }

    @RequiredUIAccess
    @Override
    public void setMinWidth(int widthInPixels) {
        myMinWidth = widthInPixels;

        applySize();
    }

    @RequiredUIAccess
    @Override
    public void setMinHeight(int heightInPixels) {
        myMinHeight = heightInPixels;

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
        return myDataObject.addListener(eventClass, listener);
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
            palette.setColor(QPalette.ColorRole.WindowText, toQColor(myForegroundColor));
            palette.setColor(QPalette.ColorRole.Text, toQColor(myForegroundColor));
            palette.setColor(QPalette.ColorRole.ButtonText, toQColor(myForegroundColor));
        }

        if (myBackgroundColor != null) {
            palette.setColor(QPalette.ColorRole.Window, toQColor(myBackgroundColor));
            palette.setColor(QPalette.ColorRole.Base, toQColor(myBackgroundColor));

            // a widget only paints its Window role when it is told to fill its own background
            myComponent.setAutoFillBackground(true);
        }

        myComponent.setPalette(palette);
    }

    protected static QColor toQColor(ColorValue colorValue) {
        RGBColor rgb = colorValue.toRGB();
        return new QColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha());
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
        return myComponent != null && myComponent.hasFocus();
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
