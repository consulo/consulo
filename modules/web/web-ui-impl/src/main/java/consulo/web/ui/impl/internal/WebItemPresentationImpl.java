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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.web.ui.impl.internal.vaadin.AuraUtility;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebItemPresentationImpl implements TextItemPresentation {
    private Image myIcon;
    private @Nullable ColorValue myBackgroundColor;
    private @Nullable Font myFont;

    private LocalizeValue mySuffixText = LocalizeValue.empty();
    private @Nullable Image mySuffixIcon;

    private LocalizeValue myTooltip = LocalizeValue.empty();

    @Override
    public TextItemPresentation withIcon(@Nullable Image image) {
        myIcon = image;

        after();
        return this;
    }

    @Override
    public TextItemPresentation withSuffix(LocalizeValue text, @Nullable Image icon) {
        mySuffixText = text;
        mySuffixIcon = icon;

        after();
        return this;
    }

    @Override
    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
        myBackgroundColor = color;

        after();
        return this;
    }

    @Override
    public TextItemPresentation withFont(Font font) {
        myFont = font;

        after();
        return this;
    }

    @Override
    public TextItemPresentation withTooltip(LocalizeValue tooltip) {
        myTooltip = tooltip;

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

    public LocalizeValue getTooltip() {
        return myTooltip;
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        // the attribute is what tells a file apart in the project view - grayed out, red for an error, the vcs
        // colour of a changed one. dropping it left every fragment looking the same
        myFragmentModels.add(new Fragment(text.get(), textAttribute));

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
    static void applyRun(Span span, String text, @Nullable TextAttribute textAttribute) {
        applyAttribute(span, textAttribute);

        Set<TextEffect> effects = textAttribute == null ? Set.of() : TextAttributeUtil.getEffects(textAttribute);
        if (text.isEmpty() || !effects.contains(TextEffect.STRIKEOUT) || !effects.contains(TextEffect.WAVED)) {
            span.setText(text);
            return;
        }

        span.setText("");
        Span strikeout = new Span(text);
        strikeout.getStyle().set("text-decoration", "line-through");
        span.add(strikeout);
    }

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

        int fontStyle = TextAttributeUtil.getFontStyle(textAttribute);
        if ((fontStyle & Font.BOLD) != 0) {
            style.set("font-weight", "bold");
        }
        if ((fontStyle & Font.ITALIC) != 0) {
            style.set("font-style", "italic");
        }

        String decoration = toTextDecoration(TextAttributeUtil.getEffects(textAttribute));
        if (decoration != null) {
            style.set("text-decoration", decoration);
        }
    }

    static void applyFont(Style style, Font font) {
        style.set("font-family", "\"" + font.getFamily() + "\", " + genericFamily(font.getFamily()));
        style.set("font-size", font.getFontSize() + "px");

        int fontStyle = font.getFontStyle();
        if ((fontStyle & Font.BOLD) != 0) {
            style.set("font-weight", "bold");
        }
        if ((fontStyle & Font.ITALIC) != 0) {
            style.set("font-style", "italic");
        }
    }

    private static String genericFamily(String family) {
        return switch (family) {
            case "Monospaced", "DialogInput" -> "monospace";
            case "Serif" -> "serif";
            default -> "sans-serif";
        };
    }

    private static @Nullable String toTextDecoration(Set<TextEffect> effects) {
        if (effects.contains(TextEffect.WAVED)) {
            return "underline wavy";
        }

        boolean strikeout = effects.contains(TextEffect.STRIKEOUT);
        boolean underline = effects.contains(TextEffect.UNDERLINE);
        if (strikeout && underline) {
            return "line-through underline";
        }
        if (strikeout) {
            return "line-through";
        }
        return underline ? "underline" : null;
    }


    @Override
    public void clearText() {
        myFragmentModels.clear();

        after();
    }

    /**
     * Builds the components anew on every call. A presentation computed once and drawn many times - a tree row
     * rebuilt as the grid scrolls - would otherwise hand the same spans to two parents, and vaadin moves an
     * element rather than sharing it, so the row built first would lose its text.
     */
    public Component toComponent() {
        Component component = buildComponent();

        Font font = myFont;
        if (font != null) {
            applyFont(component.getElement().getStyle(), font);
        }

        String tooltip = myTooltip.get();
        if (!tooltip.isEmpty()) {
            component.getElement().setAttribute("title", tooltip);
        }
        return component;
    }

    private Component buildComponent() {
        Span span = new Span();
        span.addClassName("web-icon");
        if (myIcon != null) {
            Component image = WebImageConverter.getImage(myIcon);
            image.addClassName(AuraUtility.Margin.Right.SMALL);
            span.add(image);
        }

        boolean hasSuffix = mySuffixText.isNotEmpty() || mySuffixIcon != null;
        for (Fragment fragment : myFragmentModels) {
            Span text = new Span();
            applyRun(text, fragment.text(), fragment.attribute());
            if (hasSuffix) {
                text.getStyle()
                    .set("min-width", "0")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("white-space", "nowrap");
            }
            span.add(text);
        }

        if (!hasSuffix) {
            return span;
        }

        span.getStyle()
            .set("min-width", "0")
            .set("overflow", "hidden");

        Span row = new Span();
        row.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("width", "100%");

        Span suffix = new Span();
        suffix.getStyle()
            .set("margin-left", "auto")
            .set("padding-left", "16px")
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("flex", "none");

        if (mySuffixText.isNotEmpty()) {
            Span suffixText = new Span(mySuffixText.get());
            applyAttribute(suffixText, TextAttribute.GRAYED);
            suffix.add(suffixText);
        }

        if (mySuffixIcon != null) {
            Component image = WebImageConverter.getImage(mySuffixIcon);
            if (mySuffixText.isNotEmpty()) {
                image.addClassName(AuraUtility.Margin.Left.SMALL);
            }
            suffix.add(image);
        }

        row.add(span, suffix);
        return row;
    }

    protected void after() {
    }
}
