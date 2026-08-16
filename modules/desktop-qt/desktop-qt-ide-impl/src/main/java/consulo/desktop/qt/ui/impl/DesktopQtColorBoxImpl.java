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

import consulo.ui.ColorBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.localize.UILocalize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QCursor;
import io.qt.widgets.QColorDialog;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtColorBoxImpl extends QtComponentDelegate<QPushButton> implements ColorBox {
    private static final String STYLE_SHEET = """
        QPushButton {
            background: %s;
            color: %s;
            border: 1px solid palette(mid);
            border-radius: 3px;
            padding: 2px 8px;
            min-width: 56px;
        }
        """;

    private @Nullable ColorValue myValue;

    private boolean myEditable = true;

    public DesktopQtColorBoxImpl(@Nullable ColorValue colorValue) {
        myValue = colorValue;
    }

    @Override
    protected QPushButton createQt(QWidget parent) {
        return new QPushButton(parent);
    }

    @Override
    protected void initialize(QPushButton component) {
        super.initialize(component);

        component.setAutoDefault(false);
        component.setDefault(false);

        if (getToolTipText().get().isEmpty()) {
            component.setToolTip(UILocalize.colorPanelSelectColorDialogDescription().get());
        }

        component.clicked.connect(this::chooseColor);

        updateSwatch();
    }

    /**
     * The swatch is drawn by the style sheet rather than by the palette, because a push button of most qt styles
     * paints its own gradient over the {@code Button} role and the colour would never be seen as it was given.
     */
    private void updateSwatch() {
        if (myComponent == null) {
            return;
        }

        RGBColor rgb = myValue == null ? null : myValue.toRGB();

        if (rgb == null) {
            myComponent.setText("");

            setOwnStyleSheet(STYLE_SHEET.formatted("palette(button)", "palette(button-text)"));
        }
        else {
            myComponent.setText(toHex(rgb));

            setOwnStyleSheet(STYLE_SHEET.formatted(toCssColor(rgb), isDark(rgb) ? "#FFFFFF" : "#000000"));
        }

        myComponent.setCursor(new QCursor(myEditable ? Qt.CursorShape.PointingHandCursor : Qt.CursorShape.ArrowCursor));
    }

    @RequiredUIAccess
    private void chooseColor() {
        if (!myEditable || myComponent == null) {
            return;
        }

        QColor current = myValue == null ? new QColor(255, 255, 255) : toQColor(myValue);

        QColor selected = QColorDialog.getColor(
            current,
            myComponent.window(),
            UILocalize.colorPanelSelectColorDialogDescription().get()
        );

        // the dialog answers an invalid colour when it was cancelled, and the value has to stay as it was
        if (selected == null || !selected.isValid()) {
            return;
        }

        setValue(new RGBColor(selected.red(), selected.green(), selected.blue(), selected.alpha()), true);
    }

    @Override
    public @Nullable ColorValue getValue() {
        return myValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable ColorValue value, boolean fireListeners) {
        myValue = value;

        updateSwatch();

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
        }
    }

    @Override
    public void setEditable(boolean editable) {
        myEditable = editable;

        updateSwatch();
    }

    @Override
    public boolean isEditable() {
        return myEditable;
    }

    private static String toHex(RGBColor rgb) {
        return "#%02X%02X%02X".formatted(rgb.getRed(), rgb.getGreen(), rgb.getBlue()).toUpperCase(Locale.ENGLISH);
    }

    private static String toCssColor(RGBColor rgb) {
        return "rgba(%d, %d, %d, %d%%)".formatted(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha() * 100 / 255);
    }

    private static boolean isDark(RGBColor rgb) {
        double luminance = 0.212656 * rgb.getRed() + 0.715158 * rgb.getGreen() + 0.072186 * rgb.getBlue();
        return luminance < 128;
    }
}
