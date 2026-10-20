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
package consulo.desktop.qt.ui.impl.font;

import consulo.ui.font.Font;
import consulo.ui.font.FontFeature;
import io.qt.gui.QFont;
import io.qt.gui.QRawFont;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtFontImpl implements Font {
    private final String myFamily;
    private final String myFontName;
    private final int myFontSize;
    private final int myFontStyle;
    private final Set<FontFeature> myFeatures;

    private volatile @Nullable QRawFont myRawFont;

    public DesktopQtFontImpl(String fontName, int fontSize, int fontStyle) {
        this(fontName, fontName, fontSize, fontStyle);
    }

    public DesktopQtFontImpl(String family, String fontName, int fontSize, int fontStyle) {
        this(family, fontName, fontSize, fontStyle, null);
    }

    public DesktopQtFontImpl(String family, String fontName, int fontSize, int fontStyle, @Nullable QRawFont rawFont) {
        this(family, fontName, fontSize, fontStyle, rawFont, Set.of());
    }

    private DesktopQtFontImpl(String family,
                              String fontName,
                              int fontSize,
                              int fontStyle,
                              @Nullable QRawFont rawFont,
                              Set<FontFeature> features) {
        myFeatures = features;
        myFamily = family;
        myFontName = fontName;
        myFontSize = fontSize;
        myFontStyle = fontStyle;
        myRawFont = rawFont;
    }

    public static DesktopQtFontImpl of(QFont font) {
        int style = PLAIN;
        if (font.bold()) {
            style |= BOLD;
        }
        if (font.italic()) {
            style |= ITALIC;
        }
        return new DesktopQtFontImpl(font.family(), font.family(), font.pointSize(), style);
    }

    public QFont toQFont() {
        QFont font = new QFont(myFamily);
        if (myFontSize > 0) {
            font.setPointSize(myFontSize);
        }
        font.setBold((myFontStyle & BOLD) != 0);
        font.setItalic((myFontStyle & ITALIC) != 0);
        for (FontFeature feature : myFeatures) {
            font.setFeature(feature.getTag(), 1);
        }
        return font;
    }

    @Override
    public String getName() {
        return myFontName;
    }

    @Override
    public String getFontName() {
        return myFontName;
    }

    @Override
    public String getFamily() {
        return myFamily;
    }

    @Override
    public int getFontStyle() {
        return myFontStyle;
    }

    @Override
    public int getFontSize() {
        return myFontSize;
    }

    @Override
    public Font buildNewFont(int newSize) {
        return new DesktopQtFontImpl(myFamily, myFontName, newSize, myFontStyle, myRawFont, myFeatures);
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
        return new DesktopQtFontImpl(myFamily, myFontName, myFontSize, myFontStyle, myRawFont, Collections.unmodifiableSet(newFeatures));
    }

    @Override
    public boolean canDisplay(int codePoint) {
        QRawFont rawFont = myRawFont;
        if (rawFont == null) {
            rawFont = QRawFont.fromFont(toQFont());
            myRawFont = rawFont;
        }
        return rawFont.isValid() && rawFont.supportsCharacter(codePoint);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DesktopQtFontImpl that = (DesktopQtFontImpl) o;
        return myFontSize == that.myFontSize
            && myFontStyle == that.myFontStyle
            && myFamily.equals(that.myFamily)
            && myFontName.equals(that.myFontName)
            && myFeatures.equals(that.myFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myFamily, myFontName, myFontSize, myFontStyle, myFeatures);
    }
}
