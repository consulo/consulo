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

import consulo.desktop.qt.ui.impl.font.DesktopQtFontSize;
import consulo.desktop.qt.ui.impl.base.DesktopQtIconLabel;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.Length;
import io.qt.core.Qt;
import io.qt.gui.QFont;
import io.qt.gui.QKeySequence;
import io.qt.gui.QShortcut;
import io.qt.widgets.QAbstractButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtLabelImpl extends QtComponentDelegate<DesktopQtIconLabel> implements Label, DesktopQtIconOwner {
    private LocalizeValue myText = LocalizeValue.empty();
    private @Nullable Component myLabeledComponent;
    private @Nullable Image myImage;
    private @Nullable String mySearchHighlight;
    private @Nullable QShortcut myHighlightShortcut;

    private final Set<LabelStyle> myStyles = EnumSet.noneOf(LabelStyle.class);

    private final HorizontalAlignment myHorizontalAlignment;

    public DesktopQtLabelImpl(LocalizeValue text, HorizontalAlignment horizontalAlignment) {
        myText = text;
        myHorizontalAlignment = horizontalAlignment;
    }

    @Override
    protected DesktopQtIconLabel createQt(QWidget parent) {
        return new DesktopQtIconLabel(parent);
    }

    @Override
    protected void initialize(DesktopQtIconLabel component) {
        applyText(component);
        component.setTextAlignment(switch (myHorizontalAlignment) {
            case LEFT -> Qt.AlignmentFlag.AlignLeft;
            case CENTER -> Qt.AlignmentFlag.AlignHCenter;
            case RIGHT -> Qt.AlignmentFlag.AlignRight;
        });

        updateImage();

        updateBuddy();

        for (LabelStyle style : myStyles) {
            applyStyle(style);
        }
    }

    /**
     * A qt label only turns a {@code &} into an underline once it has a buddy to hand the key press to, so a
     * label standing on its own is given the text with the marker taken back out.
     */
    protected String currentText() {
        return myComponent != null && myComponent.buddy() != null ? QtMnemonic.withMnemonic(myText) : QtMnemonic.plain(myText);
    }

    protected Qt.TextFormat textFormat() {
        return Qt.TextFormat.AutoText;
    }

    protected @Nullable String highlightedText(String query) {
        return DesktopQtSearchHighlight.toRichText(QtMnemonic.plain(myText), query);
    }

    protected @Nullable QKeySequence mnemonicShortcut() {
        QKeySequence sequence = QKeySequence.mnemonic(QtMnemonic.withMnemonic(myText));
        return sequence.isEmpty() ? null : sequence;
    }

    private void applyText(DesktopQtIconLabel component) {
        String query = mySearchHighlight;
        String highlighted = query == null ? null : highlightedText(query);
        if (highlighted != null) {
            component.setTextFormat(Qt.TextFormat.RichText);
            component.setText(highlighted);
        }
        else {
            component.setTextFormat(textFormat());
            component.setText(currentText());
        }

        updateHighlightShortcut(component, highlighted != null);
    }

    private void updateHighlightShortcut(DesktopQtIconLabel component, boolean highlighted) {
        QKeySequence sequence = highlighted && component.buddy() != null ? mnemonicShortcut() : null;

        QShortcut shortcut = myHighlightShortcut;
        if (sequence == null) {
            if (shortcut != null && !shortcut.isDisposed()) {
                shortcut.setEnabled(false);
            }
            return;
        }

        if (shortcut == null || shortcut.isDisposed()) {
            shortcut = new QShortcut(component);
            shortcut.activated.connect(() -> activateBuddy(component, true));
            shortcut.activatedAmbiguously.connect(() -> activateBuddy(component, false));
            myHighlightShortcut = shortcut;
        }

        shortcut.setKey(sequence);
        shortcut.setEnabled(true);
    }

    private static void activateBuddy(DesktopQtIconLabel component, boolean click) {
        if (component.isDisposed()) {
            return;
        }

        QWidget buddy = component.buddy();
        if (buddy == null || buddy.isDisposed()) {
            return;
        }

        if (buddy.focusPolicy() != Qt.FocusPolicy.NoFocus) {
            buddy.setFocus(Qt.FocusReason.ShortcutFocusReason);
        }

        if (click && buddy instanceof QAbstractButton button) {
            button.animateClick();
        }
    }

    @Override
    public void disposeQt() {
        myHighlightShortcut = null;

        super.disposeQt();
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;

        if (myComponent != null) {
            applyText(myComponent);
        }
    }

    @RequiredUIAccess
    @Override
    public void setSearchHighlight(@Nullable String query) {
        mySearchHighlight = SearchHighlightUtil.normalizeQuery(query);

        if (myComponent != null) {
            applyText(myComponent);
        }
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return mySearchHighlight;
    }

    @Override
    public void setImage(@Nullable Image icon) {
        myImage = icon;

        updateImage();
    }

    private void updateImage() {
        if (myComponent != null) {
            myComponent.setImage(myImage);
        }
    }

    @Override
    public void refreshIcons() {
        if (myImage != null) {
            updateImage();
        }
    }

    @Override
    public @Nullable Image getImage() {
        return myImage;
    }

    @Override
    public void setTarget(@Nullable Component component) {
        myLabeledComponent = component;

        updateBuddy();
    }

    private void updateBuddy() {
        if (myComponent == null || !(myLabeledComponent instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.whenBound(widget -> {
            if (myComponent != null) {
                myComponent.setBuddy(widget);
                applyText(myComponent);
            }
        });
    }

    @Override
    public @Nullable Component getTarget() {
        return myLabeledComponent;
    }

    @Override
    public void addStyle(LabelStyle style) {
        myStyles.add(style);

        applyStyle(style);
    }

    private void applyStyle(LabelStyle style) {
        if (myComponent == null) {
            return;
        }

        switch (style) {
            case TRANSPARENT_BACKGROUND -> myComponent.setAutoFillBackground(false);
            case FONT_BOLD -> {
                QFont font = myComponent.font();
                font.setBold(true);
                myComponent.setFont(font);
            }
            default -> myComponent.setFont(DesktopQtFontSize.withPointDelta(myComponent.font(), style.getFontPointDelta()));
        }
    }

    @RequiredUIAccess
    @Override
    public void setWidth(Length width) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void setHeight(Length height) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void setMinHeight(Length height) {
        throw new UnsupportedOperationException();
    }
}
