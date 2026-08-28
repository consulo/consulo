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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.internal.UIInternal;
import consulo.ui.util.TraverseUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public interface ColorPickerBuilder {
    static ColorPickerBuilder create() {
        return UIInternal.get()._ColorPicker_create();
    }

    ColorPickerBuilder withTitle(LocalizeValue title);

    ColorPickerBuilder withColor(@Nullable ColorValue color);

    ColorPickerBuilder withAlpha();

    ColorPickerBuilder withAlphaAsPercent();

    ColorPickerBuilder disableRecentColors();

    ColorPickerBuilder disablePipette();

    ColorPickerBuilder onColorChanged(Consumer<ColorValue> consumer);

    @RequiredUIAccess
    CompletableFuture<ColorValue> showAsync(@Nullable Window parent);

    @RequiredUIAccess
    default CompletableFuture<ColorValue> showAsync(@Nullable Component component) {
        return showAsync(TraverseUtil.getWindowAncestor(component));
    }

    @RequiredUIAccess
    default CompletableFuture<ColorValue> showPopupAsync(Component target, int x, int y, int anchorHeight) {
        return showAsync(target);
    }

    @RequiredUIAccess
    default CompletableFuture<ColorValue> showPopupAsync(Component target) {
        return showPopupAsync(target, 0, 0, 0);
    }
}
