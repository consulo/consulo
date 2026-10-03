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
package consulo.it.internal.ui;


import consulo.localize.LocalizeValue;
import consulo.ui.ColorPickerBuilder;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.ScriptedDialogs;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessColorPickerBuilder implements ColorPickerBuilder {
    private LocalizeValue myTitle = LocalizeValue.empty();
    private @Nullable ColorValue myColor;
    private boolean myWithAlpha;
    private boolean myAlphaAsPercent;
    private boolean myRecentColors = true;
    private boolean myPipette = true;
    private @Nullable Consumer<ColorValue> myColorChangedConsumer;

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
        myWithAlpha = true;
        return this;
    }

    @Override
    public ColorPickerBuilder withAlphaAsPercent() {
        myWithAlpha = true;
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
        myColorChangedConsumer = consumer;
        return this;
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<ColorValue> showAsync(@Nullable Window parent) {
        ScriptedDialogs dialogs = ScriptedDialogs.installed();
        if (dialogs == null) {
            return failed("no dialog script is installed");
        }

        dialogs.record(this);

        Object answer = dialogs.nextAnswer();
        if (answer instanceof ScriptedDialogs.InputAnswer input) {
            Object value = input.value();
            if (value == null) {
                return CompletableFuture.completedFuture(null);
            }
            if (value instanceof ColorValue color) {
                Consumer<ColorValue> consumer = myColorChangedConsumer;
                if (consumer != null) {
                    consumer.accept(color);
                }
                return CompletableFuture.completedFuture(color);
            }
            return failed("color picker answered with " + value);
        }

        if (dialogs.unexpected() == ScriptedDialogs.Unexpected.DISMISS) {
            return CompletableFuture.completedFuture(null);
        }

        return failed("unexpected color picker: " + myTitle.get());
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public @Nullable ColorValue getColor() {
        return myColor;
    }

    public boolean isWithAlpha() {
        return myWithAlpha;
    }

    public boolean isAlphaAsPercent() {
        return myAlphaAsPercent;
    }

    public boolean isRecentColors() {
        return myRecentColors;
    }

    public boolean isPipette() {
        return myPipette;
    }

    private static CompletableFuture<ColorValue> failed(String message) {
        CompletableFuture<ColorValue> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException(message));
        return result;
    }
}
