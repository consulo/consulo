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
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.color.RGBColor;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class CodeReviewLightEditorColorSchemeExtender implements EditorColorSchemeExtender {
    @Override
    public void extend(Builder builder) {
        builder.add(CodeReviewColorUtil.REVIEW_CHANGED_LINES_COLOR, new RGBColor(0xF8, 0xA0, 0xDF));
        builder.add(CodeReviewColorUtil.REVIEW_STATE_FOREGROUND, new RGBColor(0x6C, 0x70, 0x7E));
        builder.add(CodeReviewColorUtil.REVIEW_STATE_BACKGROUND, new RGBColor(0xDF, 0xE1, 0xE5));
        builder.add(CodeReviewColorUtil.REVIEW_CHAT_ITEM_HOVER, new RGBColor(0xDF, 0xE1, 0xE5, 0x33));
        builder.add(CodeReviewColorUtil.REVIEW_LINE_FRAME_BORDER, new RGBColor(0x35, 0x74, 0xF0));
        builder.add(CodeReviewColorUtil.REVIEW_INLAY_BORDER, new RGBColor(0xEB, 0xEC, 0xF0));
        builder.add(CodeReviewColorUtil.REVIEW_TIMELINE_DIFF_ANCHOR_LINE, AttributesFlyweightBuilder.create()
            .withBackground(new RGBColor(0xFB, 0xF1, 0xD1))
            .build());
    }

    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DEFAULT_SCHEME_NAME;
    }
}
