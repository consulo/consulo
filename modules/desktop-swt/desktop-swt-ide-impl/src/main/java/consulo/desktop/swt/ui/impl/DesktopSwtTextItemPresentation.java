/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtTextItemPresentation implements TextItemPresentation {
  private List<LocalizeValue> myValues = new ArrayList<>();
  private Image myImage;

  public Image getImage() {
    return myImage;
  }

  @Nonnull
  @Override
  public TextItemPresentation withIcon(@Nullable Image image) {
    myImage = image;
    return this;
  }

  @Override
  public void clearText() {
    myValues.clear();
  }

  @Override
  public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
    myValues.add(text);
  }

  public String toString() {
    return StringUtil.join(myValues, LocalizeValue::get, "");
  }
}
