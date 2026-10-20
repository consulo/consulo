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

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.impl.SpeedSearchSupport;
import consulo.ui.style.ComponentColors;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QKeyEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class DesktopQtSpeedSearch<Item> {
    private static final int IDLE_MS = 5000;
    private static final String PUNCTUATION = "*_-\"'/.#$>:,;?!@%^&";
    private static final String ACTIVE_PROPERTY = "consuloSpeedSearchActive";
    private static final String HINT_NAME = "consuloSpeedSearchHint";

    private final SpeedSearchSupport<Item> mySupport;
    private final Supplier<List<Item>> myItems;
    private final Supplier<@Nullable Item> mySelected;
    private final Consumer<Item> mySelect;

    private @Nullable QFrame myHint;
    private @Nullable QLabel myHintText;
    private int myGeneration;

    public DesktopQtSpeedSearch(boolean camelCaseWords,
                                Supplier<List<Item>> items,
                                Supplier<@Nullable Item> selected,
                                Consumer<Item> select) {
        mySupport = new SpeedSearchSupport<>(camelCaseWords);
        myItems = items;
        mySelected = selected;
        mySelect = select;
    }

    public static boolean isActive(QWidget widget) {
        return Boolean.TRUE.equals(widget.property(ACTIVE_PROPERTY));
    }

    public void install(QAbstractItemView view) {
        disposeHint();
        mySupport.setText(null);

        view.installEventFilter(new QObject(view) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (type == QEvent.Type.KeyPress && event instanceof QKeyEvent keyEvent) {
                    return keyPressed(view, keyEvent);
                }

                if (type == QEvent.Type.FocusOut || type == QEvent.Type.Hide) {
                    close(view);
                }
                return false;
            }
        });
    }

    public void setConverter(@Nullable Function<Item, String> converter) {
        mySupport.setConverter(converter);

        if (converter == null) {
            myGeneration++;
            disposeHint();
        }
    }

    public @Nullable String getText() {
        return mySupport.getText();
    }

    private boolean keyPressed(QAbstractItemView view, QKeyEvent event) {
        if (!mySupport.isEnabled()) {
            return false;
        }

        Qt.KeyboardModifiers modifiers = event.modifiers();
        if (modifiers.testFlag(Qt.KeyboardModifier.ControlModifier)
            || modifiers.testFlag(Qt.KeyboardModifier.AltModifier)
            || modifiers.testFlag(Qt.KeyboardModifier.MetaModifier)) {
            return false;
        }

        int key = event.key();
        String typed = event.text();
        String text = mySupport.getText();
        if (text == null) {
            if (typed.length() == 1 && startsSearch(typed.charAt(0))) {
                update(view, typed, 0);
                return true;
            }
            return false;
        }

        if (key == Qt.Key.Key_Escape.value()) {
            close(view);
            return true;
        }

        if (key == Qt.Key.Key_Backspace.value()) {
            update(view, text.isEmpty() ? text : text.substring(0, text.length() - 1), 0);
            return true;
        }

        if (key == Qt.Key.Key_Up.value()
            || key == Qt.Key.Key_Down.value()
            || key == Qt.Key.Key_Home.value()
            || key == Qt.Key.Key_End.value()) {
            if (modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier)) {
                return false;
            }
            update(view, text, key);
            return true;
        }

        if (key == Qt.Key.Key_Return.value()
            || key == Qt.Key.Key_Enter.value()
            || key == Qt.Key.Key_Tab.value()
            || key == Qt.Key.Key_Backtab.value()
            || key == Qt.Key.Key_PageUp.value()
            || key == Qt.Key.Key_PageDown.value()
            || key == Qt.Key.Key_Left.value()
            || key == Qt.Key.Key_Right.value()) {
            close(view);
            return false;
        }

        if (!typed.isEmpty() && !Character.isISOControl(typed.charAt(0))) {
            update(view, text + typed, 0);
            return true;
        }
        return false;
    }

    private void update(QAbstractItemView view, String text, int key) {
        mySupport.setText(text);
        view.setProperty(ACTIVE_PROPERTY, true);

        List<Item> items = myItems.get();
        Item selected = mySelected.get();
        int selectedIndex = SpeedSearchSupport.indexOf(items, selected);

        Item found;
        if (key == Qt.Key.Key_Up.value()) {
            found = mySupport.findPrevious(items, selectedIndex);
        }
        else if (key == Qt.Key.Key_Down.value()) {
            found = mySupport.findNext(items, selectedIndex);
        }
        else if (key == Qt.Key.Key_Home.value()) {
            found = mySupport.findFirst(items);
        }
        else if (key == Qt.Key.Key_End.value()) {
            found = mySupport.findLast(items);
        }
        else {
            found = mySupport.find(items, selectedIndex);
        }

        if (found != null && !Objects.equals(found, selected)) {
            mySelect.accept(found);
        }

        showHint(view, text, found != null);

        int generation = ++myGeneration;
        QTimer.singleShot(IDLE_MS, view, () -> {
            if (generation == myGeneration && !view.isDisposed()) {
                close(view);
            }
        });
    }

    private void close(QAbstractItemView view) {
        myGeneration++;

        if (mySupport.getText() != null) {
            mySupport.setText(null);
        }

        if (!view.isDisposed()) {
            view.setProperty(ACTIVE_PROPERTY, false);
        }

        disposeHint();
    }

    private void showHint(QAbstractItemView view, String text, boolean found) {
        QWidget window = view.window();

        QFrame hint = myHint;
        QLabel label = myHintText;
        if (hint == null || label == null || hint.isDisposed() || label.isDisposed() || hint.parentWidget() != window) {
            disposeHint();

            hint = new QFrame(window);
            hint.setObjectName(HINT_NAME);
            hint.setFocusPolicy(Qt.FocusPolicy.NoFocus);
            hint.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents);
            hint.setStyleSheet("#" + HINT_NAME + " {"
                + " background-color: " + css(TargetQt.to(ComponentColors.TOOLTIP_BACKGROUND)) + ";"
                + " border: 1px solid " + css(TargetQt.to(ComponentColors.BORDER)) + ";"
                + " }");

            QHBoxLayout layout = new QHBoxLayout(hint);
            layout.setContentsMargins(4, 2, 4, 2);
            layout.setSpacing(4);

            QLabel icon = new QLabel(hint);
            int iconSize = view.fontMetrics().height();
            icon.setPixmap(DesktopQtImage.toQIcon(PlatformIconGroup.actionsSearch()).pixmap(iconSize, iconSize));
            layout.addWidget(icon);

            label = new QLabel(hint);
            label.setTextFormat(Qt.TextFormat.PlainText);
            layout.addWidget(label);

            myHint = hint;
            myHintText = label;
        }

        ComponentColors foreground = found ? ComponentColors.TOOLTIP_FOREGROUND : ComponentColors.ERROR_FOREGROUND;
        label.setStyleSheet("color: " + css(TargetQt.to(foreground)) + ";");
        label.setText(text);
        hint.adjustSize();

        QPoint origin = view.mapTo(window, new QPoint(0, 0));
        int top = origin.y() - hint.height();
        hint.move(Math.max(0, origin.x()), top < 0 ? Math.max(0, origin.y()) : top);
        hint.show();
        hint.raise();
    }

    private void disposeHint() {
        QFrame hint = myHint;
        myHint = null;
        myHintText = null;

        if (hint != null && !hint.isDisposed()) {
            hint.hide();
            hint.disposeLater();
        }
    }

    private static boolean startsSearch(char c) {
        return Character.isLetterOrDigit(c) || PUNCTUATION.indexOf(c) >= 0;
    }

    private static String css(QColor color) {
        return "rgba(" + color.red() + ", " + color.green() + ", " + color.blue() + ", " + color.alpha() + ")";
    }
}
