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
package consulo.desktop.qt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTextItemPresentation implements TextItemPresentation {
    private final List<DesktopQtTextFragment> myFragments = new ArrayList<>();
    private @Nullable Image myImage;
    private @Nullable ColorValue myBackgroundColor;
    private LocalizeValue mySuffixText = LocalizeValue.empty();
    private @Nullable Image mySuffixImage;

    public @Nullable Image getImage() {
        return myImage;
    }

    public List<DesktopQtTextFragment> getFragments() {
        return myFragments;
    }

    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    public LocalizeValue getSuffixText() {
        return mySuffixText;
    }

    public @Nullable Image getSuffixImage() {
        return mySuffixImage;
    }

    public boolean hasSuffix() {
        return mySuffixText.isNotEmpty() || mySuffixImage != null;
    }

    @Override
    public TextItemPresentation withIcon(@Nullable Image image) {
        myImage = image;
        return this;
    }

    @Override
    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
        myBackgroundColor = color;
        return this;
    }

    @Override
    public TextItemPresentation withSuffix(LocalizeValue text, @Nullable Image icon) {
        mySuffixText = text;
        mySuffixImage = icon;
        return this;
    }

    @Override
    public void clearText() {
        myFragments.clear();
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        myFragments.add(new DesktopQtTextFragment(text, textAttribute));
    }

    @Override
    public String toString() {
        return StringUtil.join(myFragments, fragment -> fragment.text().get(), "");
    }
}
