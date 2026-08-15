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
import consulo.ui.ex.SimpleTextAttributes;
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
    private @Nullable ColorValue myBackgroundColor;

    @Override
    public TextItemPresentation withIcon(@Nullable Image image) {
        myIcon = image;

        after();
        return this;
    }

    @Override
    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
        myBackgroundColor = color;

        after();
        return this;
    }

    /**
     * What each run says and how it is drawn, kept beside the span built from it - a consumer which owns its spans
     * already can bind these onto the ones it has rather than take new ones.
     */
    public record Fragment(String text, @Nullable TextAttribute attribute) {
    }

    private final List<Fragment> myFragmentModels = new ArrayList<>();

    public List<Fragment> getFragments() {
        return myFragmentModels;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        String string = text.get();
        Span span = new Span(string);

        // the attribute is what tells a file apart in the project view - grayed out, red for an error, the vcs
        // colour of a changed one. dropping it left every fragment looking the same
        applyAttribute(span, textAttribute);

        myFragments.add(span);
        myFragmentModels.add(new Fragment(string, textAttribute));

        after();
    }

    /**
     * The fill of the item as a whole, if one was assigned. A consumer whose whole surface is the item - an
     * editor tab, a tree row - paints it over that surface rather than behind the run of text alone.
     */
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    /**
     * Every property, always - a span may be one a consumer is reusing, and one which merely goes unset keeps
     * whatever the run before it asked for. A completion row rebound from an item with a tail onto one without kept
     * showing the old tail for exactly this reason.
     */
    static void applyAttribute(Span span, @Nullable TextAttribute textAttribute) {
        Style style = span.getStyle();

        style.remove("color");
        style.remove("background-color");
        style.remove("font-weight");
        style.remove("font-style");
        style.remove("text-decoration");

        if (textAttribute == null) {
            return;
        }

        ColorValue foreground = textAttribute.getForegroundColor();
        if (foreground != null) {
            style.set("color", WebColors.toCssColor(foreground));
        }

        ColorValue background = textAttribute.getBackgroundColor();
        if (background != null) {
            style.set("background-color", WebColors.toCssColor(background));
        }

        int fontStyle = textAttribute.getStyle();
        if ((fontStyle & Font.BOLD) != 0) {
            style.set("font-weight", "bold");
        }
        if ((fontStyle & Font.ITALIC) != 0) {
            style.set("font-style", "italic");
        }

        // one css property covers both, so a fragment carrying the two has to ask for them together
        boolean strikeout = (fontStyle & SimpleTextAttributes.STYLE_STRIKEOUT) != 0;
        boolean underline = (fontStyle & SimpleTextAttributes.STYLE_UNDERLINE) != 0;
        if (strikeout || underline) {
            style.set("text-decoration", strikeout && underline ? "line-through underline" : strikeout ? "line-through" : "underline");
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
