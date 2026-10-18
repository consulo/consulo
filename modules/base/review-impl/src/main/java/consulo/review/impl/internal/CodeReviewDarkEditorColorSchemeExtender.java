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

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.color.RGBColor;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class CodeReviewDarkEditorColorSchemeExtender implements EditorColorSchemeExtender {
    @Override
    public void extend(Builder builder) {
        builder.add(CodeReviewColorUtil.REVIEW_CHANGED_LINES_COLOR, new RGBColor(0x8A, 0x41, 0x75));
        builder.add(CodeReviewColorUtil.REVIEW_STATE_FOREGROUND, new RGBColor(0x86, 0x8A, 0x91));
        builder.add(CodeReviewColorUtil.REVIEW_STATE_BACKGROUND, new RGBColor(0x43, 0x45, 0x4A));
        builder.add(CodeReviewColorUtil.REVIEW_CHAT_ITEM_HOVER, new RGBColor(0x5A, 0x5D, 0x63, 0x33));
        builder.add(CodeReviewColorUtil.REVIEW_LINE_FRAME_BORDER, new RGBColor(0x54, 0x8A, 0xF7));
        builder.add(CodeReviewColorUtil.REVIEW_INLAY_BORDER, new RGBColor(0x39, 0x3B, 0x40));
    }

    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DARCULA_SCHEME_NAME;
    }
}
