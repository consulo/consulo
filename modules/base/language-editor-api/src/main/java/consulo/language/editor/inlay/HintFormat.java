// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

public final class HintFormat {
    private final HintColorKind colorKind;
    private final HintFontSize fontSize;
    private final HintMarginPadding horizontalMarginPadding;

    public static final HintFormat DEFAULT = new HintFormat(
        HintColorKind.Default,
        HintFontSize.AsInEditor,
        HintMarginPadding.OnlyPadding
    );

    public HintFormat(HintColorKind colorKind,
                      HintFontSize fontSize,
                      HintMarginPadding horizontalMarginPadding) {
        this.colorKind = colorKind;
        this.fontSize = fontSize;
        this.horizontalMarginPadding = horizontalMarginPadding;
    }

    public HintColorKind getColorKind() {
        return colorKind;
    }

    public HintFontSize getFontSize() {
        return fontSize;
    }

    public HintMarginPadding getHorizontalMarginPadding() {
        return horizontalMarginPadding;
    }

    public HintFormat withColorKind(HintColorKind newColorKind) {
        return new HintFormat(newColorKind, this.fontSize, this.horizontalMarginPadding);
    }

    public HintFormat withFontSize(HintFontSize newFontSize) {
        return new HintFormat(this.colorKind, newFontSize, this.horizontalMarginPadding);
    }

    public HintFormat withHorizontalMargin(HintMarginPadding newHorizontalMarginPadding) {
        return new HintFormat(this.colorKind, this.fontSize, newHorizontalMarginPadding);
    }
}
