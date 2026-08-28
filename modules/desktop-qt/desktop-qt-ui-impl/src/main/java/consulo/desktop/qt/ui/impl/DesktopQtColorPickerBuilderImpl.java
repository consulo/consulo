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

import consulo.localize.LocalizeValue;
import consulo.ui.ColorPickerBuilder;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.localize.UILocalize;
import io.qt.gui.QColor;
import io.qt.widgets.QApplication;
import io.qt.widgets.QColorDialog;
import io.qt.widgets.QDialog;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public class DesktopQtColorPickerBuilderImpl implements ColorPickerBuilder {
    private LocalizeValue myTitle = UILocalize.colorPanelSelectColorDialogDescription();

    private @Nullable ColorValue myColor;

    private @Nullable Consumer<ColorValue> myColorChanged;

    private boolean myAlpha;

    private boolean myPipetteDisabled;

    @Override
    public ColorPickerBuilder withTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public ColorPickerBuilder withColor(@Nullable ColorValue color) {
        myColor = color;
        return this;
    }

    @Override
    public ColorPickerBuilder withAlpha() {
        myAlpha = true;
        return this;
    }

    @Override
    public ColorPickerBuilder withAlphaAsPercent() {
        return withAlpha();
    }

    @Override
    public ColorPickerBuilder disableRecentColors() {
        return this;
    }

    @Override
    public ColorPickerBuilder disablePipette() {
        myPipetteDisabled = true;
        return this;
    }

    @Override
    public ColorPickerBuilder onColorChanged(Consumer<ColorValue> consumer) {
        myColorChanged = consumer;
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<ColorValue> showAsync(@Nullable Window parent) {
        CompletableFuture<ColorValue> result = new CompletableFuture<>();

        QWidget parentWidget = parent == null ? QApplication.activeWindow() : TargetQt.to(parent);

        QColorDialog dialog = new QColorDialog(parentWidget);
        dialog.setWindowTitle(myTitle.get());
        dialog.setOption(QColorDialog.ColorDialogOption.ShowAlphaChannel, myAlpha);
        dialog.setOption(QColorDialog.ColorDialogOption.NoEyeDropperButton, myPipetteDisabled);

        if (myColor != null) {
            dialog.setCurrentColor(TargetQt.to(myColor));
        }

        Consumer<ColorValue> colorChanged = myColorChanged;
        if (colorChanged != null) {
            dialog.currentColorChanged.connect(color -> {
                if (color != null && color.isValid()) {
                    colorChanged.accept(TargetQt.from(color));
                }
            });
        }

        dialog.finished.connect(code -> {
            QColor selected = dialog.selectedColor();

            boolean accepted = code == QDialog.DialogCode.Accepted.value();

            result.complete(accepted && selected != null && selected.isValid() ? TargetQt.from(selected) : null);

            dialog.disposeLater();
        });

        dialog.destroyed.connect(() -> {
            if (!result.isDone()) {
                result.complete(null);
            }
        });

        dialog.open();

        return result;
    }
}
