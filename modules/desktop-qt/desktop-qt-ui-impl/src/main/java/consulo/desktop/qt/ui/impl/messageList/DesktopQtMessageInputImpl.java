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
package consulo.desktop.qt.ui.impl.messageList;

import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageInputSubmitEvent;
import consulo.ui.style.ComponentColors;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFocusEvent;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QPlainTextEdit;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
public class DesktopQtMessageInputImpl extends QtComponentDelegate<QFrame> implements MessageInput {
    private static final int MAX_ROWS = 6;

    private class FieldFrame extends QFrame {
        FieldFrame(QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            if (myInplace) {
                QPainter painter = new QPainter(this);
                try {
                    painter.fillRect(rect(), palette().color(QPalette.ColorRole.Base));
                }
                finally {
                    painter.end();
                }
                return;
            }
            QPlainTextEdit edit = myEdit;
            boolean focused = edit != null && edit.hasFocus();
            QPalette palette = palette();
            QColor border = focused
                ? DesktopQtStyleApplier.themeColor(ComponentColors.FOCUS_COLOR, palette.color(QPalette.ColorRole.Highlight))
                : DesktopQtStyleApplier.themeColor(ComponentColors.BORDER, palette.color(QPalette.ColorRole.Mid));

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                double stroke = focused ? 2 : 1;
                QPen pen = new QPen(border);
                pen.setWidthF(stroke);
                painter.setPen(pen);
                painter.setBrush(palette.color(QPalette.ColorRole.Base));
                painter.drawRoundedRect(new QRectF(stroke / 2, stroke / 2, width() - stroke, height() - stroke), 6, 6);
            }
            finally {
                painter.end();
            }
        }
    }

    private class InputEdit extends QPlainTextEdit {
        InputEdit(QWidget parent) {
            super(parent);
        }

        @Override
        protected void keyPressEvent(QKeyEvent event) {
            int key = event.key();
            boolean enter = key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value();
            if (enter && !event.modifiers().testFlag(Qt.KeyboardModifier.ShiftModifier)) {
                submit();
                return;
            }
            super.keyPressEvent(event);
        }

        @Override
        protected void focusInEvent(QFocusEvent event) {
            super.focusInEvent(event);
            repaintFrame();
        }

        @Override
        protected void focusOutEvent(QFocusEvent event) {
            super.focusOutEvent(event);
            repaintFrame();
        }
    }

    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private static final String DEFAULT_SEND_TEXT = "\u27A4";

    private LocalizeValue mySendText = LocalizeValue.empty();
    private @Nullable Image mySendIcon;
    private @Nullable Component mySuffixComponent;
    private boolean myInplace;
    private @Nullable QHBoxLayout myLayout;

    private @Nullable QPlainTextEdit myEdit;
    private @Nullable QPushButton mySendButton;

    @Override
    protected QFrame createQt(QWidget parent) {
        FieldFrame frame = new FieldFrame(parent);
        QHBoxLayout layout = new QHBoxLayout(frame);
        layout.setContentsMargins(8, 3, 4, 3);
        layout.setSpacing(4);

        InputEdit edit = new InputEdit(frame);
        edit.setFrameShape(QFrame.Shape.NoFrame);
        edit.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
        edit.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
        edit.setLineWrapMode(QPlainTextEdit.LineWrapMode.WidgetWidth);
        edit.setTabChangesFocus(true);
        edit.setAutoFillBackground(false);
        edit.viewport().setAutoFillBackground(false);
        layout.addWidget(edit, 1);

        QPushButton send = new QPushButton(frame);
        send.setFlat(true);
        send.setFocusPolicy(Qt.FocusPolicy.NoFocus);
        send.setCursor(Qt.CursorShape.PointingHandCursor);
        QColor link = DesktopQtStyleApplier.themeColor(ComponentColors.LINK_FOREGROUND, send.palette().color(QPalette.ColorRole.Link));
        send.setStyleSheet("QPushButton { color: " + link.name() + "; border: none; padding: 2px 6px; }"
            + " QPushButton:disabled { color: palette(placeholder-text); }");
        send.clicked.connect(this::submit);
        layout.addWidget(send, 0, Qt.AlignmentFlag.AlignVCenter);

        myEdit = edit;
        mySendButton = send;
        myLayout = layout;
        attachSuffix(frame);

        edit.textChanged.connect(this::onTextChanged);
        applyTexts();
        onTextChanged();
        return frame;
    }

    private void repaintFrame() {
        QFrame frame = toQtComponent();
        if (frame != null && !frame.isDisposed()) {
            frame.update();
        }
    }

    private void onTextChanged() {
        QPlainTextEdit edit = myEdit;
        QPushButton send = mySendButton;
        if (edit == null || send == null) {
            return;
        }
        send.setEnabled(edit.isEnabled() && !edit.toPlainText().isBlank());

        QFontMetrics metrics = edit.fontMetrics();
        int documentLines = (int) Math.ceil(edit.document().size().height());
        int lines = Math.max(1, Math.min(MAX_ROWS, documentLines));
        edit.setVerticalScrollBarPolicy(documentLines > MAX_ROWS ? Qt.ScrollBarPolicy.ScrollBarAsNeeded : Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
        int margins = (int) Math.ceil(edit.document().documentMargin() * 2) + edit.contentsMargins().top() + edit.contentsMargins().bottom();
        edit.setFixedHeight(lines * metrics.lineSpacing() + margins);
    }

    private void applyTexts() {
        QPlainTextEdit edit = myEdit;
        QPushButton send = mySendButton;
        if (edit != null) {
            edit.setPlaceholderText(myPlaceholder.get());
        }
        if (send != null) {
            String text = mySendText == LocalizeValue.empty() ? "" : mySendText.get();
            Image icon = mySendIcon;
            if (icon != null) {
                send.setIcon(DesktopQtImage.toQIcon(icon));
                send.setText("");
                send.setToolTip(text);
            }
            else {
                send.setIcon(DesktopQtImage.toQIcon(null));
                send.setText(text.isEmpty() ? DEFAULT_SEND_TEXT : text);
                send.setToolTip("");
            }
            send.setAccessibleName(text);
        }
    }

    @RequiredUIAccess
    private void submit() {
        QPlainTextEdit edit = myEdit;
        if (edit == null || !edit.isEnabled()) {
            return;
        }
        String text = edit.toPlainText();
        if (text.isBlank()) {
            return;
        }
        edit.clear();
        getListenerDispatcher(MessageInputSubmitEvent.class).onEvent(new MessageInputSubmitEvent(this, text));
    }

    @RequiredUIAccess
    @Override
    public void setPlaceholder(LocalizeValue placeholder) {
        myPlaceholder = placeholder;
        applyTexts();
    }

    @RequiredUIAccess
    @Override
    public void setSendText(LocalizeValue sendText) {
        mySendText = sendText;
        applyTexts();
    }

    @Override
    public void addStyle(MessageInputStyle style) {
        switch (style) {
            case INPLACE -> {
                myInplace = true;
                QFrame frame = toQtComponent();
                if (frame != null && !frame.isDisposed()) {
                    frame.update();
                }
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        Component old = mySuffixComponent;
        if (old == suffixComponent) {
            return;
        }
        if (old instanceof QtComponentDelegate<?> delegate) {
            QWidget widget = delegate.toQtComponent();
            QHBoxLayout layout = myLayout;
            if (widget != null && layout != null && !widget.isDisposed()) {
                layout.removeWidget(widget);
            }
            delegate.setParent(null);
        }
        mySuffixComponent = suffixComponent;
        QFrame frame = toQtComponent();
        if (frame != null && !frame.isDisposed()) {
            attachSuffix(frame);
        }
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    private void attachSuffix(QFrame frame) {
        QHBoxLayout layout = myLayout;
        QPushButton send = mySendButton;
        if (!(mySuffixComponent instanceof QtComponentDelegate<?> delegate) || layout == null || send == null) {
            return;
        }
        delegate.setParent(this);
        delegate.bind(frame, null);
        QWidget widget = delegate.toQtComponent();
        if (widget != null) {
            layout.insertWidget(layout.indexOf(send), widget, 0, Qt.AlignmentFlag.AlignVCenter);
            widget.show();
        }
    }

    @RequiredUIAccess
    @Override
    public void setSendIcon(@Nullable Image icon) {
        mySendIcon = icon;
        applyTexts();
    }
}
