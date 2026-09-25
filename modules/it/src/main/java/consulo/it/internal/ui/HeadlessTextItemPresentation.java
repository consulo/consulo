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

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public class HeadlessTextItemPresentation implements TextItemPresentation {
    private final List<LocalizeValue> myTexts = new ArrayList<>();
    private final List<TextAttribute> myAttributes = new ArrayList<>();

    private @Nullable Image myIcon;
    private @Nullable ColorValue myBackgroundColor;
    private LocalizeValue mySuffix = LocalizeValue.empty();

    @Override
    public TextItemPresentation withIcon(@Nullable Image image) {
        myIcon = image;
        return this;
    }

    @Override
    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
        myBackgroundColor = color;
        return this;
    }

    @Override
    public TextItemPresentation withSuffix(LocalizeValue text, @Nullable Image icon) {
        mySuffix = text;
        return this;
    }

    @Override
    public void clearText() {
        myTexts.clear();
        myAttributes.clear();
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        myTexts.add(text);
        myAttributes.add(textAttribute);
    }

    public String getText() {
        StringBuilder builder = new StringBuilder();
        for (LocalizeValue text : myTexts) {
            builder.append(text.get());
        }
        return builder.toString();
    }

    public List<TextAttribute> getAttributes() {
        return List.copyOf(myAttributes);
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    public LocalizeValue getSuffix() {
        return mySuffix;
    }
}
