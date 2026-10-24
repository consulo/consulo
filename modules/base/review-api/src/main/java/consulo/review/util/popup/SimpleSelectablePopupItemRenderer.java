// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.util.popup;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.RenderItem;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRenderer;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import java.util.function.Function;

public final class SimpleSelectablePopupItemRenderer<T> implements TextItemRenderer<T> {
    private final boolean myShowCheckbox;
    private final Function<T, SelectablePopupItemPresentation> myPresenter;

    private SimpleSelectablePopupItemRenderer(boolean showCheckbox, Function<T, SelectablePopupItemPresentation> presenter) {
        myShowCheckbox = showCheckbox;
        myPresenter = presenter;
    }

    @Override
    public void render(TextItemPresentation presentation, RenderItem<T> item) {
        T value = item.getValue();
        if (value == null) {
            return;
        }

        SelectablePopupItemPresentation itemPresentation = myPresenter.apply(value);

        Image icon = itemPresentation.getIcon();
        if (myShowCheckbox) {
            Image check = itemPresentation.isSelected() ? PlatformIconGroup.actionsChecked() : ImageEffects.empty(16);
            icon = icon == null ? check : ImageEffects.appendRight(check, icon);
        }
        if (icon != null) {
            presentation.withIcon(icon);
        }
        presentation.append(itemPresentation.getShortText());
    }

    public static <T> TextItemRenderer<T> create(Function<T, SelectablePopupItemPresentation> presenter) {
        return create(true, presenter);
    }

    public static <T> TextItemRenderer<T> create(boolean showCheckboxes, Function<T, SelectablePopupItemPresentation> presenter) {
        return new SimpleSelectablePopupItemRenderer<>(showCheckboxes, presenter);
    }
}
