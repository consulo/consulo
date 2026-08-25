/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex;

import consulo.annotation.DeprecationInfo;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.Comparing;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
@SuppressWarnings({"PointlessBitwiseExpression"})
public final class SimpleTextAttributes {

    @MagicConstant(flags = {STYLE_PLAIN, STYLE_BOLD, STYLE_ITALIC, STYLE_STRIKEOUT, STYLE_WAVED, STYLE_UNDERLINE, STYLE_BOLD_DOTTED_LINE, STYLE_SEARCH_MATCH, STYLE_SMALLER, STYLE_OPAQUE})
    public @interface StyleAttributeConstant {
    }

    public static final int STYLE_PLAIN = Font.PLAIN;
    public static final int STYLE_BOLD = Font.BOLD;
    public static final int STYLE_ITALIC = Font.ITALIC;
    public static final int FONT_MASK = STYLE_PLAIN | STYLE_BOLD | STYLE_ITALIC;
    public static final int STYLE_STRIKEOUT = STYLE_ITALIC << 1;
    public static final int STYLE_WAVED = STYLE_STRIKEOUT << 1;
    public static final int STYLE_UNDERLINE = STYLE_WAVED << 1;
    public static final int STYLE_BOLD_DOTTED_LINE = STYLE_UNDERLINE << 1;
    public static final int STYLE_SEARCH_MATCH = STYLE_BOLD_DOTTED_LINE << 1;
    public static final int STYLE_SMALLER = STYLE_SEARCH_MATCH << 1;
    public static final int STYLE_OPAQUE = STYLE_SMALLER << 1;

    public static final SimpleTextAttributes REGULAR_ATTRIBUTES = of(STYLE_PLAIN, null);
    public static final SimpleTextAttributes REGULAR_BOLD_ATTRIBUTES = of(STYLE_BOLD, null);
    public static final SimpleTextAttributes REGULAR_ITALIC_ATTRIBUTES = of(STYLE_ITALIC, null);
    public static final SimpleTextAttributes ERROR_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.ERROR_FOREGROUND);
    public static final SimpleTextAttributes ERROR_BOLD_ATTRIBUTES = of(STYLE_BOLD, ComponentColors.ERROR_FOREGROUND);

    public static final SimpleTextAttributes GRAY_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.INFO_FOREGROUND);
    public static final SimpleTextAttributes GRAY_ITALIC_ATTRIBUTES = of(STYLE_ITALIC, ComponentColors.INFO_FOREGROUND);
    public static final SimpleTextAttributes GRAY_SMALL_ATTRIBUTES = of(STYLE_SMALLER, ComponentColors.INFO_FOREGROUND);

    public static final SimpleTextAttributes GRAYED_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.DISABLED_TEXT);
    public static final SimpleTextAttributes GRAYED_BOLD_ATTRIBUTES = of(STYLE_BOLD, ComponentColors.DISABLED_TEXT);
    public static final SimpleTextAttributes GRAYED_ITALIC_ATTRIBUTES = of(STYLE_ITALIC, ComponentColors.DISABLED_TEXT);
    public static final SimpleTextAttributes GRAYED_SMALL_ATTRIBUTES = of(STYLE_SMALLER, ComponentColors.DISABLED_TEXT);

    public static final SimpleTextAttributes SYNTHETIC_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.LINK_FOREGROUND);
    public static final SimpleTextAttributes DARK_TEXT = of(STYLE_PLAIN, ComponentColors.TEXT_FOREGROUND);
    public static final SimpleTextAttributes SIMPLE_CELL_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.TEXT_FOREGROUND);
    public static final SimpleTextAttributes SELECTED_SIMPLE_CELL_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.SELECTION_FOREGROUND);
    public static final SimpleTextAttributes EXCLUDED_ATTRIBUTES = of(STYLE_ITALIC, ComponentColors.INFO_FOREGROUND);

    public static final SimpleTextAttributes LINK_PLAIN_ATTRIBUTES = of(STYLE_PLAIN, ComponentColors.LINK_FOREGROUND);
    public static final SimpleTextAttributes LINK_ATTRIBUTES = of(STYLE_UNDERLINE, ComponentColors.LINK_FOREGROUND);
    public static final SimpleTextAttributes LINK_BOLD_ATTRIBUTES = of(STYLE_UNDERLINE | STYLE_BOLD, ComponentColors.LINK_FOREGROUND);

    private final ColorValue myBgColor;
    private final ColorValue myFgColor;
    private final ColorValue myWaveColor;
    @StyleAttributeConstant
    private final int myStyle;

    public static SimpleTextAttributes of(@StyleAttributeConstant int style, @Nullable ColorValue fgColor) {
        return of(style, fgColor, null);
    }

    public static SimpleTextAttributes of(@StyleAttributeConstant int style, @Nullable ColorValue fgColor, @Nullable ColorValue waveColor) {
        return of(null, fgColor, waveColor, style);
    }

    public static SimpleTextAttributes of(
        @Nullable ColorValue bgColor,
        @Nullable ColorValue fgColor,
        @Nullable ColorValue waveColor,
        @StyleAttributeConstant int style
    ) {
        return new SimpleTextAttributes(bgColor, fgColor, waveColor, style);
    }

    private SimpleTextAttributes(
        @Nullable ColorValue bgColor,
        @Nullable ColorValue fgColor,
        @Nullable ColorValue waveColor,
        @StyleAttributeConstant int style
    ) {
        if ((~(STYLE_PLAIN |
            STYLE_BOLD |
            STYLE_ITALIC |
            STYLE_STRIKEOUT |
            STYLE_WAVED |
            STYLE_UNDERLINE |
            STYLE_BOLD_DOTTED_LINE |
            STYLE_SEARCH_MATCH |
            STYLE_SMALLER |
            STYLE_OPAQUE) & style) != 0) {
            throw new IllegalArgumentException("Wrong style: " + style);
        }

        myBgColor = bgColor;
        myFgColor = fgColor;
        myWaveColor = waveColor;
        myStyle = style;
    }

    /**
     * @param style   style of the text fragment.
     * @param fgColor color of the text fragment. <code>color</code> can be
     *                <code>null</code>. In that case <code>SimpleColoredComponent</code> will
     *                use its foreground to paint the text fragment.
     */
    @Deprecated
    @DeprecationInfo("Use of() with ColorValue")
    public SimpleTextAttributes(@StyleAttributeConstant int style, Color fgColor) {
        this(style, fgColor, null);
    }

    @Deprecated
    @DeprecationInfo("Use of() with ColorValue")
    public SimpleTextAttributes(@StyleAttributeConstant int style, Color fgColor, @Nullable Color waveColor) {
        this(null, fgColor, waveColor, style);
    }

    @Deprecated
    @DeprecationInfo("Use of() with ColorValue")
    public SimpleTextAttributes(@Nullable Color bgColor,
                                Color fgColor,
                                @Nullable Color waveColor,
                                @StyleAttributeConstant int style) {
        this(TargetAWT.from(bgColor), TargetAWT.from(fgColor), TargetAWT.from(waveColor), style);
    }

    public @Nullable ColorValue foreground() {
        return myFgColor;
    }

    public @Nullable ColorValue background() {
        return myBgColor;
    }

    /**
     * <code>null</code> means that color of wave is the same as foreground color.
     */
    public @Nullable ColorValue wave() {
        return myWaveColor;
    }

    /**
     * @return foreground color
     */
    @Deprecated
    @DeprecationInfo("Use foreground()")
    public Color getFgColor() {
        return TargetAWT.to(myFgColor);
    }

    /**
     * @return background color
     */
    @Deprecated
    @DeprecationInfo("Use background()")
    public @Nullable Color getBgColor() {
        return TargetAWT.to(myBgColor);
    }

    /**
     * @return wave color. The method can return <code>null</code>. <code>null</code>
     * means that color of wave is the same as foreground color.
     */
    @Deprecated
    @DeprecationInfo("Use wave()")
    public @Nullable Color getWaveColor() {
        return TargetAWT.to(myWaveColor);
    }

    @StyleAttributeConstant
    public int getStyle() {
        return myStyle;
    }

    /**
     * @return whether text is struck out or not
     */
    public boolean isStrikeout() {
        return (myStyle & STYLE_STRIKEOUT) != 0;
    }

    /**
     * @return whether text is waved or not
     */
    public boolean isWaved() {
        return (myStyle & STYLE_WAVED) != 0;
    }

    public boolean isUnderline() {
        return (myStyle & STYLE_UNDERLINE) != 0;
    }

    public boolean isBoldDottedLine() {
        return (myStyle & STYLE_BOLD_DOTTED_LINE) != 0;
    }

    public boolean isSearchMatch() {
        return (myStyle & STYLE_SEARCH_MATCH) != 0;
    }

    public boolean isSmaller() {
        return (myStyle & STYLE_SMALLER) != 0;
    }

    public boolean isOpaque() {
        return (myStyle & STYLE_OPAQUE) != 0;
    }

    public int getFontStyle() {
        return myStyle & FONT_MASK;
    }

    @Deprecated
    @DeprecationInfo("Use deriveColors()")
    public SimpleTextAttributes derive(@StyleAttributeConstant int style, @Nullable Color fg, @Nullable Color bg, @Nullable Color wave) {
        return deriveColors(style, TargetAWT.from(fg), TargetAWT.from(bg), TargetAWT.from(wave));
    }

    public SimpleTextAttributes deriveColors(
        @StyleAttributeConstant int style,
        @Nullable ColorValue fg,
        @Nullable ColorValue bg,
        @Nullable ColorValue wave
    ) {
        return of(
            bg != null ? bg : background(),
            fg != null ? fg : foreground(),
            wave != null ? wave : wave(),
            style == -1 ? getStyle() : style
        );
    }

    // take what differs from REGULAR
    public static SimpleTextAttributes merge(SimpleTextAttributes weak, SimpleTextAttributes strong) {
        int style;
        if (strong.getStyle() != REGULAR_ATTRIBUTES.getStyle()) {
            style = strong.getStyle();
        }
        else {
            style = weak.getStyle();
        }
        ColorValue wave;
        if (!Comparing.equal(strong.wave(), REGULAR_ATTRIBUTES.wave())) {
            wave = strong.wave();
        }
        else {
            wave = weak.wave();
        }
        ColorValue fg;
        if (!Comparing.equal(strong.foreground(), REGULAR_ATTRIBUTES.foreground())) {
            fg = strong.foreground();
        }
        else {
            fg = weak.foreground();
        }
        ColorValue bg;
        if (!Comparing.equal(strong.background(), REGULAR_ATTRIBUTES.background())) {
            bg = strong.background();
        }
        else {
            bg = weak.background();
        }

        return of(bg, fg, wave, style);
    }
}
