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
package consulo.it.internal.ui;

import consulo.ui.font.Font;
import consulo.ui.font.FontFeature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Dummy-but-creatable headless {@link Font}.
 *
 * @author VISTALL
 */
public class HeadlessFont implements Font {
    private final String myName;
    private final int mySize;
    private final int myStyle;
    private final Set<FontFeature> myFeatures;

    public HeadlessFont(String name, int size, int style) {
        this(name, size, style, Set.of());
    }

    private HeadlessFont(String name, int size, int style, Set<FontFeature> features) {
        myFeatures = features;
        myName = name;
        mySize = size;
        myStyle = style;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getFontName() {
        return myName;
    }

    @Override
    public String getFamily() {
        return myName;
    }

    @Override
    public int getFontStyle() {
        return myStyle;
    }

    @Override
    public int getFontSize() {
        return mySize;
    }

    @Override
    public Font buildNewFont(int newSize) {
        return new HeadlessFont(myName, newSize, myStyle, myFeatures);
    }

    @Override
    public Set<FontFeature> getFeatures() {
        return myFeatures;
    }

    @Override
    public Font withFeatures(FontFeature... features) {
        EnumSet<FontFeature> newFeatures = myFeatures.isEmpty() ? EnumSet.noneOf(FontFeature.class) : EnumSet.copyOf(myFeatures);
        Collections.addAll(newFeatures, features);
        return new HeadlessFont(myName, mySize, myStyle, Collections.unmodifiableSet(newFeatures));
    }

    @Override
    public boolean canDisplay(int codePoint) {
        return false;
    }
}
