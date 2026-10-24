/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.ui.impl.internal.vaadin;

import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.WebColors;
import consulo.web.ui.impl.internal.WebSearchHighlight;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
@Tag("span")
public abstract class VaadinLabelComponentBase extends SimpleComponent implements FromVaadinComponentWrapper, HasText, HasEnabled, HasSize {
    private Image myImage;
    private LocalizeValue myText;
    private HorizontalAlignment myHorizontalAlignment;
    private ColorValue myForegroundColor;
    private @Nullable String mySearchHighlight;

    public VaadinLabelComponentBase() {
        // a span wraps and nothing of the label api asks for more than one line - the status bar widget reporting a
        // selection broke over four lines and pushed the whole bar taller
        getStyle().set("white-space", "nowrap");
    }

    public void setImage(Image image) {
        myImage = image;

        updateContent();
    }

    public Image getImage() {
        return myImage;
    }

    public void setText(LocalizeValue text) {
        myText = text;

        updateContent();
    }

    /**
     * {@link HasText#setText} replaces every child of the element, so an icon and a text cannot both survive it.
     * The content is assembled here instead, and the interface method routes through the same path.
     */
    @Override
    public void setText(String text) {
        setText(LocalizeValue.of(text == null ? "" : text));
    }

    @Override
    public String getText() {
        return myText == null ? "" : LocalizeValueWithMnemonic.get(myText).getText();
    }

    public LocalizeValue getTextValue() {
        return myText;
    }

    public void setSearchHighlight(@Nullable String query) {
        mySearchHighlight = query;

        updateContent();
    }

    public @Nullable String getSearchHighlight() {
        return mySearchHighlight;
    }

    protected void updateContent() {
        getElement().removeAllChildren();

        if (myImage != null) {
            getElement().appendChild(WebImageConverter.getImage(myImage).getElement());
        }

        String text = getText();
        if (!text.isEmpty()) {
            WebSearchHighlight.appendText(getElement(), text, mySearchHighlight);
        }

        getElement().getClassList().set("web-label-with-image", myImage != null && !text.isEmpty());
    }

    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        myHorizontalAlignment = horizontalAlignment;

        if (horizontalAlignment != null) {
            getStyle().set("text-align", switch (horizontalAlignment) {
                case LEFT -> "left";
                case CENTER -> "center";
                case RIGHT -> "right";
            });
            getStyle().set("justify-content", switch (horizontalAlignment) {
                case LEFT -> "flex-start";
                case CENTER -> "center";
                case RIGHT -> "flex-end";
            });
        }
    }

    @Override
    public void setHeight(@Nullable String height) {
        super.setHeight(height);
        getElement().getClassList().set("web-label-filled", height != null && !height.isEmpty());
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return myHorizontalAlignment;
    }

    public void setForegroundColor(ColorValue foregroundColor) {
        myForegroundColor = foregroundColor;

        if (foregroundColor == null) {
            getStyle().remove("color");
        }
        else {
            getStyle().set("color", WebColors.toStyleCssColor(foregroundColor));
        }
    }

    public ColorValue getForegroundColor() {
        return myForegroundColor;
    }
}
