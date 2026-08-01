/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import consulo.web.internal.ui.vaadin.AuraUtility;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.web.internal.ui.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebItemPresentationImpl implements TextItemPresentation {
    private Image myIcon;
    private List<Component> myFragments = new ArrayList<>();

    @Override
    public TextItemPresentation withIcon(@Nullable Image image) {
        myIcon = image;

        after();
        return this;
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        Span span = new Span(text.get());

        // the attribute is what tells a file apart in the project view - grayed out, red for an error, the vcs
        // colour of a changed one. dropping it left every fragment looking the same
        applyAttribute(span, textAttribute);

        myFragments.add(span);

        after();
    }

    private static void applyAttribute(Span span, @Nullable TextAttribute textAttribute) {
        if (textAttribute == null) {
            return;
        }

        Style style = span.getStyle();

        ColorValue foreground = textAttribute.getForegroundColor();
        if (foreground != null) {
            style.set("color", WebColors.toCssColor(foreground));
        }

        ColorValue background = textAttribute.getBackgroundColor();
        if (background != null) {
            style.set("background-color", WebColors.toCssColor(background));
        }

        int fontStyle = textAttribute.getStyle();
        if ((fontStyle & Font.STYLE_BOLD) != 0) {
            style.set("font-weight", "bold");
        }
        if ((fontStyle & Font.STYLE_ITALIC) != 0) {
            style.set("font-style", "italic");
        }
    }


    @Override
    public void clearText() {
        myFragments.clear();

        after();
    }

    public Component toComponent() {
        Span span = new Span();
        span.addClassName("web-icon");
        if (myIcon != null) {
            Component image = WebImageConverter.getImage(myIcon);
            image.addClassName(AuraUtility.Margin.Right.SMALL);
            span.add(image);
        }
        span.add(myFragments.toArray(Component[]::new));
        return span;
    }

    protected void after() {
    }
}
