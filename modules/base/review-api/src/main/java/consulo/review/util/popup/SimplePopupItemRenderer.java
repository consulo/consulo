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

import consulo.ui.RenderItem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRenderer;
import consulo.ui.image.Image;

import java.util.function.Function;

public final class SimplePopupItemRenderer<T> implements TextItemRenderer<T> {
    private final Function<T, PopupItemPresentation> myPresenter;

    private SimplePopupItemRenderer(Function<T, PopupItemPresentation> presenter) {
        myPresenter = presenter;
    }

    @Override
    public void render(TextItemPresentation presentation, RenderItem<T> item) {
        T value = item.getValue();
        if (value == null) {
            return;
        }

        PopupItemPresentation itemPresentation = myPresenter.apply(value);
        Image icon = itemPresentation.getIcon();
        if (icon != null) {
            presentation.withIcon(icon);
        }
        String fullText = itemPresentation.getFullText();
        if (fullText != null) {
            presentation.append(fullText);
            presentation.append(" ");
            presentation.append(itemPresentation.getShortText(), TextAttribute.GRAYED);
        }
        else {
            presentation.append(itemPresentation.getShortText());
        }
    }

    public static <T> TextItemRenderer<T> create(Function<T, PopupItemPresentation> presenter) {
        return new SimplePopupItemRenderer<>(presenter);
    }
}
