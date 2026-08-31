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
package consulo.desktop.awt.ui.impl.color;

import consulo.localize.LocalizeValue;
import consulo.ui.ColorPickerBuilder;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public class DesktopAWTColorPickerBuilder implements ColorPickerBuilder {
    private LocalizeValue myTitle = LocalizeValue.empty();
    private @Nullable ColorValue myColor;
    private boolean myAlpha;
    private boolean myAlphaAsPercent;
    private boolean myRecentColors = true;
    private boolean myPipette = true;
    private @Nullable Consumer<ColorValue> myColorConsumer;

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
        myAlpha = true;
        myAlphaAsPercent = true;
        return this;
    }

    @Override
    public ColorPickerBuilder disableRecentColors() {
        myRecentColors = false;
        return this;
    }

    @Override
    public ColorPickerBuilder disablePipette() {
        myPipette = false;
        return this;
    }

    @Override
    public ColorPickerBuilder onColorChanged(Consumer<ColorValue> consumer) {
        myColorConsumer = consumer;
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<ColorValue> showAsync(@Nullable Window parent) {
        return show(parent == null ? null : TargetAWT.to(parent), null);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<ColorValue> showPopupAsync(consulo.ui.Component target, int x, int y, int anchorHeight) {
        Component awtTarget = TargetAWT.to(target);
        if (awtTarget == null || !awtTarget.isShowing()) {
            return showAsync(target);
        }

        Point location = new Point(x, y + anchorHeight);
        SwingUtilities.convertPointToScreen(location, awtTarget);

        return show(awtTarget, location);
    }

    @RequiredUIAccess
    private CompletableFuture<ColorValue> show(@Nullable Component awtParent, @Nullable Point screenLocation) {
        CompletableFuture<ColorValue> result = new CompletableFuture<>();

        DesktopColorPicker.ColorPickerDialog dialog = new DesktopColorPicker.ColorPickerDialog(
            awtParent,
            myTitle.get(),
            TargetAWT.to(myColor),
            myAlpha,
            myAlphaAsPercent,
            myRecentColors,
            myPipette
        );

        if (myColorConsumer != null) {
            Consumer<ColorValue> consumer = myColorConsumer;
            dialog.setColorConsumer(color -> consumer.accept(TargetAWT.from(color)));
        }

        if (screenLocation != null) {
            dialog.setInitialLocationCallback(() -> screenLocation);
        }

        SwingUtilities.invokeLater(() -> {
            dialog.show();

            if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                result.complete(TargetAWT.from(dialog.getColor()));
            }
            else {
                result.complete(null);
            }
        });

        return result;
    }
}
