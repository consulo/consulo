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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.DesktopAWTTargetAWTImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class DesktopTreeNodePresentation implements TextItemPresentation {
    private final PresentationData myPresentation;

    private @Nullable Font myFont;
    private LocalizeValue mySuffixText = LocalizeValue.empty();
    private @Nullable Image mySuffixIcon;

    DesktopTreeNodePresentation(PresentationData presentation) {
        myPresentation = presentation;
    }

    @Override
    public void clearText() {
        myPresentation.clearText();
    }

    @Override
    public TextItemPresentation withIcon(@Nullable Image image) {
        myPresentation.setIcon(image);
        return this;
    }

    @Override
    public TextItemPresentation withFont(Font font) {
        myFont = font;
        return this;
    }

    @Override
    public TextItemPresentation withBackgroundColor(@Nullable ColorValue color) {
        myPresentation.setBackground(color);
        return this;
    }

    @Override
    public TextItemPresentation withSuffix(LocalizeValue text, @Nullable Image icon) {
        mySuffixText = text;
        mySuffixIcon = icon;
        return this;
    }

    @Override
    public TextItemPresentation withTooltip(LocalizeValue tooltip) {
        myPresentation.setTooltip(tooltip.getNullIfEmpty());
        return this;
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        myPresentation.addText(text, DesktopAWTTargetAWTImpl.from(textAttribute));
    }

    DesktopTreeRowDecoration toDecoration() {
        if (myFont == null && mySuffixText.isEmpty() && mySuffixIcon == null) {
            return DesktopTreeRowDecoration.EMPTY;
        }
        return new DesktopTreeRowDecoration(myFont, mySuffixText, mySuffixIcon);
    }
}
