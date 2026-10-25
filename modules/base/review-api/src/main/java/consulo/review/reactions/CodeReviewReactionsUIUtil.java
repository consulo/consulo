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
package consulo.review.reactions;

import consulo.localize.LocalizeValue;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.StandardColors;
import consulo.util.lang.xml.XmlStringUtil;

import java.util.ArrayList;
import java.util.List;

public final class CodeReviewReactionsUIUtil {
    private static final List<String> PREFERRED_EMOJI_FONTS = List.of(
        "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji"
    );

    static final String VARIATION_SELECTOR = "️";

    public static final int BUTTON_HEIGHT = 24;

    public static final int HORIZONTAL_GAP = 8;

    public static final int ICON_SIZE = 20;
    public static final float COUNTER_FONT_SIZE = 11f;

    public static final class Picker {
        public static final int WIDTH = 358;
        public static final int HEIGHT = 415;

        public static final int BLOCK_PADDING = 5;

        private Picker() {
        }
    }

    private CodeReviewReactionsUIUtil() {
    }

    /**
     * Draws the emoji of {@code text} into an image of {@code size}, the emoji centred.
     */
    public static Image createUnicodeEmojiIcon(String text, int size) {
        String emoji = text.endsWith(VARIATION_SELECTOR) ? text : text + VARIATION_SELECTOR;
        return ImageEffects.canvas(size, size, canvas -> {
            Font font = FontManager.get().createFont(String.join(", ", PREFERRED_EMOJI_FONTS), size * 3 / 4, Font.PLAIN);
            canvas.setFont(font);
            canvas.setFillStyle(StandardColors.BLACK);
            canvas.setTextAlign(Canvas2D.TextAlign.center);
            canvas.setTextBaseline(Canvas2D.TextBaseline.middle);
            canvas.fillText(emoji, size / 2.0, size / 2.0);
        });
    }

    public static LocalizeValue createTooltipText(List<LocalizeValue> users, LocalizeValue reactionName) {
        return LocalizeValue.lazy(() -> {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < users.size(); i += 3) {
                List<String> chunk = new ArrayList<>();
                for (LocalizeValue user : users.subList(i, Math.min(i + 3, users.size()))) {
                    chunk.add("<b>" + XmlStringUtil.escapeString(user.get()) + "</b>");
                }
                lines.add(String.join(", ", chunk));
            }
            String reactors = String.join("<br>", lines) + "<br>";
            String message = ReviewLocalize.reviewCommentsReactionTooltip(reactors, reactionName.get()).get();
            return LocalizeValue.of("<html><body><div style=\"text-align: center\">" + message + "</div></body></html>");
        });
    }
}
