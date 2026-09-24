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
package consulo.execution.debug.impl.internal.frame;

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Collects the presentation of a frame or a value - which is written into a {@link consulo.ui.ex.ColoredTextContainer} -
 * so it can be shown by a {@link TextItemPresentation} of a unified list or tree.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public final class UnifiedColoredTextContainer extends SimpleColoredText {
    private @Nullable Image myIcon;

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }

    public void appendTo(TextItemPresentation presentation) {
        if (myIcon != null) {
            presentation.withIcon(myIcon);
        }

        List<String> texts = getTexts();
        List<SimpleTextAttributes> attributes = getAttributes();
        for (int i = 0; i < texts.size(); i++) {
            presentation.append(LocalizeValue.of(texts.get(i)), toTextAttribute(attributes.get(i)));
        }
    }

    public static TextAttribute toTextAttribute(@Nullable SimpleTextAttributes attributes) {
        if (attributes == null) {
            return TextAttribute.REGULAR;
        }

        int style = 0;
        if ((attributes.getStyle() & SimpleTextAttributes.STYLE_BOLD) != 0) {
            style |= TextAttribute.STYLE_BOLD;
        }
        if ((attributes.getStyle() & SimpleTextAttributes.STYLE_ITALIC) != 0) {
            style |= TextAttribute.STYLE_ITALIC;
        }
        return new TextAttribute(style, attributes.foreground(), attributes.background());
    }
}
