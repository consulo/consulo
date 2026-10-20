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
package consulo.web.ui.impl.internal;

import consulo.ui.font.Font;
import consulo.ui.font.FontFeature;
import consulo.ui.impl.font.file.FontFile;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class WebFontImpl implements Font {
    /**
     * The font a cell inherits from the theme - the browser knows it, the server does not. A font built from it
     * is applied only where it differs.
     */
    public static final WebFontImpl INHERITED = new WebFontImpl("", "", 0, PLAIN);

    private final String myFamily;
    private final String myFontName;
    private final int myFontSize;
    private final int myFontStyle;
    private final @Nullable FontFile myFontFile;
    private final Set<FontFeature> myFeatures;

    public WebFontImpl(String fontName, int fontSize, int fontStyle) {
        this(fontName, fontName, fontSize, fontStyle);
    }

    public WebFontImpl(String family, String fontName, int fontSize, int fontStyle) {
        this(family, fontName, fontSize, fontStyle, null);
    }

    public WebFontImpl(String family, String fontName, int fontSize, int fontStyle, @Nullable FontFile fontFile) {
        this(family, fontName, fontSize, fontStyle, fontFile, Set.of());
    }

    private WebFontImpl(String family, String fontName, int fontSize, int fontStyle, @Nullable FontFile fontFile, Set<FontFeature> features) {
        myFeatures = features;
        myFamily = family;
        myFontName = fontName;
        myFontSize = fontSize;
        myFontStyle = fontStyle;
        myFontFile = fontFile;
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
        return new WebFontImpl(myFamily, myFontName, newSize, myFontStyle, myFontFile, myFeatures);
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
        return new WebFontImpl(myFamily, myFontName, myFontSize, myFontStyle, myFontFile, Collections.unmodifiableSet(newFeatures));
    }

    @Override
    public boolean canDisplay(int codePoint) {
        return myFontFile == null || myFontFile.canDisplay(codePoint);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebFontImpl webFont = (WebFontImpl) o;
        return myFontSize == webFont.myFontSize && myFontStyle == webFont.myFontStyle && Objects.equals(myFamily, webFont.myFamily)
            && Objects.equals(myFontName, webFont.myFontName) && myFeatures.equals(webFont.myFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myFamily, myFontName, myFontSize, myFontStyle, myFeatures);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WebFontImpl{");
        sb.append("myFamily='").append(myFamily).append('\'');
        sb.append(", myFontName='").append(myFontName).append('\'');
        sb.append(", myFontSize=").append(myFontSize);
        sb.append(", myFontStyle=").append(myFontStyle);
        sb.append(", myFeatures=").append(myFeatures);
        sb.append('}');
        return sb.toString();
    }
}
