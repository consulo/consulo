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
import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
@Tag("span")
public abstract class VaadinLabelComponentBase extends SimpleComponent implements FromVaadinComponentWrapper, HasText, HasEnabled, HasSize {
  private Image myImage;
  private LocalizeValue myTextValue;
  private HorizontalAlignment myHorizontalAlignment;
  private ColorValue myForegroundColor;

  public void setImage(Image image) {
    myImage = image;
  }

  public Image getImage() {
    return myImage;
  }

  public void setTextValue(LocalizeValue textValue) {
    myTextValue = textValue;

    setText(textValue.getValue());
  }

  public LocalizeValue getTextValue() {
    return myTextValue;
  }

  public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
    myHorizontalAlignment = horizontalAlignment;
  }

  public HorizontalAlignment getHorizontalAlignment() {
    return myHorizontalAlignment;
  }

  public void setForegroundColor(ColorValue foregroundColor) {
    myForegroundColor = foregroundColor;
  }

  public ColorValue getForegroundColor() {
    return myForegroundColor;
  }
}
