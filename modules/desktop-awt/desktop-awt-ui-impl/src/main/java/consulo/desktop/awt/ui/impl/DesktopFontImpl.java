/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl;

import com.jetbrains.FontExtensions;
import com.jetbrains.JBR;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.font.Font;
import consulo.ui.font.FontFeature;
import consulo.util.lang.BitUtil;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public final class DesktopFontImpl implements Font {
    private static final Supplier<@Nullable FontExtensions> FONT_EXTENSIONS = LazyValue.nullable(() -> JBR.getFontExtensions());

    private final java.awt.Font myFont;
    private final int myFontStyle;
    private final Set<FontFeature> myFeatures;

    public DesktopFontImpl(String fontName, int fontSize, int fontStyle) {
        myFontStyle = fontStyle;

        int style = 0;
        style = BitUtil.set(style, java.awt.Font.PLAIN, BitUtil.isSet(fontStyle, Font.PLAIN));
        style = BitUtil.set(style, java.awt.Font.BOLD, BitUtil.isSet(fontStyle, Font.BOLD));
        style = BitUtil.set(style, java.awt.Font.ITALIC, BitUtil.isSet(fontStyle, Font.ITALIC));

        myFont = new java.awt.Font(fontName, style, JBUI.scaleFontSize(fontSize));
        myFeatures = Set.of();
    }

    public DesktopFontImpl(java.awt.Font font) {
        this(font, Set.of());
    }

    private DesktopFontImpl(java.awt.Font font, Set<FontFeature> features) {
        myFont = font;
        myFeatures = features;

        int result = 0;
        result = BitUtil.set(result, Font.PLAIN, BitUtil.isSet(font.getStyle(), java.awt.Font.PLAIN));
        result = BitUtil.set(result, Font.BOLD, BitUtil.isSet(font.getStyle(), java.awt.Font.BOLD));
        result = BitUtil.set(result, Font.ITALIC, BitUtil.isSet(font.getStyle(), java.awt.Font.ITALIC));

        myFontStyle = result;
    }

    public java.awt.Font getFont() {
        return myFont;
    }

    @Override
    public String getName() {
        return myFont.getFontName();
    }

    @Override
    public String getFontName() {
        return myFont.getFontName();
    }

    @Override
    public String getFamily() {
        return myFont.getFamily();
    }

    @Override
    public int getFontStyle() {
        return myFontStyle;
    }

    @Override
    public int getFontSize() {
        return myFont.getSize();
    }

    @Override
    public Font buildNewFont(int newSize) {
        java.awt.Font newFont = myFont.deriveFont((float) newSize);
        return new DesktopFontImpl(applyFeatures(newFont, myFeatures), myFeatures);
    }

    @Override
    public Set<FontFeature> getFeatures() {
        return myFeatures;
    }

    @Override
    public Font withFeatures(FontFeature... features) {
        EnumSet<FontFeature> newFeatures = myFeatures.isEmpty() ? EnumSet.noneOf(FontFeature.class) : EnumSet.copyOf(myFeatures);
        Collections.addAll(newFeatures, features);
        if (newFeatures.equals(myFeatures)) {
            return this;
        }
        return new DesktopFontImpl(applyFeatures(myFont, newFeatures), Collections.unmodifiableSet(newFeatures));
    }

    private static java.awt.Font applyFeatures(java.awt.Font font, Set<FontFeature> features) {
        if (features.isEmpty()) {
            return font;
        }
        FontExtensions fontExtensions = FONT_EXTENSIONS.get();
        if (fontExtensions == null) {
            return font;
        }
        return fontExtensions.deriveFontWithFeatures(font, features.stream().map(FontFeature::getTag).toArray(String[]::new));
    }

    @Override
    public boolean canDisplay(int codePoint) {
        return myFont.canDisplay(codePoint);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DesktopFontImpl that = (DesktopFontImpl) o;
        return Objects.equals(myFont, that.myFont) && myFeatures.equals(that.myFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myFont, myFeatures);
    }

    @Override
    public String toString() {
        return myFont.toString();
    }
}
