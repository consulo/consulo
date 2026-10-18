// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;

/**
 * Review colours live in the editor colour scheme, so every frontend resolves them the same way and a scheme can
 * restyle them.
 */
public final class CodeReviewColorUtil {
    public static final EditorColorKey REVIEW_CHANGED_LINES_COLOR =
        EditorColorKey.createColorKey("REVIEW_CHANGED_LINES_COLOR", new RGBColor(0xF8, 0xA0, 0xDF));
    public static final EditorColorKey REVIEW_STATE_FOREGROUND =
        EditorColorKey.createColorKey("REVIEW_STATE_FOREGROUND", new RGBColor(0x6C, 0x70, 0x7E));
    public static final EditorColorKey REVIEW_STATE_BACKGROUND =
        EditorColorKey.createColorKey("REVIEW_STATE_BACKGROUND", new RGBColor(0xDF, 0xE1, 0xE5));
    public static final EditorColorKey REVIEW_CHAT_ITEM_HOVER =
        EditorColorKey.createColorKey("REVIEW_CHAT_ITEM_HOVER", new RGBColor(0xDF, 0xE1, 0xE5, 0x33));
    public static final EditorColorKey REVIEW_LINE_FRAME_BORDER =
        EditorColorKey.createColorKey("REVIEW_LINE_FRAME_BORDER", new RGBColor(0x35, 0x74, 0xF0));
    public static final EditorColorKey REVIEW_INLAY_BORDER =
        EditorColorKey.createColorKey("REVIEW_INLAY_BORDER", new RGBColor(0xEB, 0xEC, 0xF0));

    /**
     * What an editor inlay stands on - the editor's own background, under its frame, padding and content alike.
     */
    public static final ColorValue EDITOR_BACKGROUND =
        ColorValue.lazy(() -> EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());

    private CodeReviewColorUtil() {
    }
}
