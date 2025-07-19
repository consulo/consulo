/*
 * Copyright 2013-2025 consulo.io
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
package consulo.bookmark.internal;

import consulo.bookmark.icon.BookmarkIconGroup;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-07-19
 */
public class BookmarkIcon {

    //0..9  + A..Z
    // Gutter + Action icon
    @SuppressWarnings("unchecked")
    private static final Couple<Image>[] ourMnemonicImageCache = new Couple[36];

    @Nonnull
    public static Image getDefaultIcon(boolean gutter) {
        return gutter ? BookmarkIconGroup.gutterBookmark() : BookmarkIconGroup.actionBookmark();
    }

    @Nonnull
    public static Image getMnemonicIcon(char mnemonic, boolean gutter) {
        int index = mnemonic - 48;
        if (index > 9) {
            index -= 7;
        }
        if (index < 0 || index > ourMnemonicImageCache.length - 1) {
            return createMnemonicIcon(mnemonic, gutter);
        }

        if (ourMnemonicImageCache[index] == null) {
            // its not mistake about using gutter icon as default icon for named bookmarks, too big icon
            ourMnemonicImageCache[index] = Couple.of(createMnemonicIcon(mnemonic, true), createMnemonicIcon(mnemonic, true));
        }
        Couple<Image> couple = ourMnemonicImageCache[index];
        return gutter ? couple.getFirst() : couple.getSecond();
    }

    @Nonnull
    private static Image createMnemonicIcon(char cha, boolean gutter) {
        ImageKey base = PlatformIconGroup.gutterMnemonic();

        return ImageEffects.layered(base, ImageEffects.canvas(base.getWidth(), base.getHeight(), c -> {
            c.setFillStyle(ComponentColors.TEXT);

            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            int editorFontSize = scheme.getEditorFontSize();

            c.setFont(FontManager.get().createFont(scheme.getEditorFontName(), editorFontSize - 2, Font.STYLE_PLAIN));
            c.setTextAlign(Canvas2D.TextAlign.center);
            c.setTextBaseline(Canvas2D.TextBaseline.middle);

            c.fillText(Character.toString(cha), base.getWidth() / 2 - 1, base.getHeight() / 2 - 2);
        }));
    }
}
