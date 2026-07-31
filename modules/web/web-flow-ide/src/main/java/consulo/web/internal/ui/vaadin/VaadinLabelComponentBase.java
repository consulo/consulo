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
package consulo.web.internal.ui.vaadin;

import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.image.WebImageConverter;

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
        return myText == null ? "" : TextWithMnemonic.parse(myText.get()).getText();
    }

    public LocalizeValue getTextValue() {
        return myText;
    }

    private void updateContent() {
        getElement().removeAllChildren();

        if (myImage != null) {
            getElement().appendChild(WebImageConverter.getImage(myImage).getElement());
        }

        String text = getText();
        if (!text.isEmpty()) {
            getElement().appendChild(Element.createText(text));
        }
    }

    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        myHorizontalAlignment = horizontalAlignment;

        if (horizontalAlignment != null) {
            getStyle().set("text-align", switch (horizontalAlignment) {
                case LEFT -> "left";
                case CENTER -> "center";
                case RIGHT -> "right";
            });
        }
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
            RGBColor color = foregroundColor.toRGB();
            getStyle().set("color", "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")");
        }
    }

    public ColorValue getForegroundColor() {
        return myForegroundColor;
    }
}
