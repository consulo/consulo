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
package consulo.ui.ex.awt;

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.util.lang.BitUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-08
 */
public class ColoredTextItemPresentation implements TextItemPresentation {
    private final SimpleColoredComponent myComponent;

    public ColoredTextItemPresentation(SimpleColoredComponent component) {
        myComponent = component;
    }

    public static SimpleTextAttributes toSimpleTextAttributes(TextAttribute textAttribute) {
        int style = textAttribute.getStyle();

        int mask = 0;
        mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_PLAIN, BitUtil.isSet(style, TextAttribute.STYLE_PLAIN));
        mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_BOLD, BitUtil.isSet(style, TextAttribute.STYLE_BOLD));
        mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_ITALIC, BitUtil.isSet(style, TextAttribute.STYLE_ITALIC));
        mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_STRIKEOUT, BitUtil.isSet(style, Font.STYLE_STRIKEOUT));
        mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_UNDERLINE, BitUtil.isSet(style, Font.STYLE_UNDERLINE));

        return new SimpleTextAttributes(
            mask,
            TargetAWT.to(textAttribute.getForegroundColor()),
            TargetAWT.to(textAttribute.getBackgroundColor())
        );
    }

    @Override
    public void clearText() {
        Image icon = myComponent.getIcon();
        myComponent.clear();
        myComponent.setIcon(icon);
    }

    @Override
    public TextItemPresentation withFont(Font font) {
        myComponent.setFont(TargetAWT.to(font));
        return this;
    }

    @Override
    public TextItemPresentation withIcon(@Nullable Image icon) {
        myComponent.setIcon(icon);
        return this;
    }

    @Override
    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
        myComponent.setBackground(TargetAWT.to(color));
        return this;
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        myComponent.append(text, toSimpleTextAttributes(textAttribute));
    }
}
