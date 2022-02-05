/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language;

import consulo.util.xml.serializer.annotation.Attribute;
import consulo.annotation.DeprecationInfo;
import consulo.component.extension.AbstractExtensionPointBean;
import consulo.component.extension.KeyedLazyInstance;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StandardColors;
import consulo.util.lang.lazy.LazyValue;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 12:27/08.10.13
 */
public class LanguageElementIcon extends AbstractExtensionPointBean implements KeyedLazyInstance<Image> {

  // these must be public for scrambling compatibility
  @Attribute("language")
  public String language;

  @Attribute("file")
  @Deprecated
  @DeprecationInfo("Use 'icon' attribute")
  public String file;

  @Attribute("icon")
  public String icon;

  @SuppressWarnings("deprecation")
  private Supplier<Image> myIconValue = LazyValue.nullable(() -> {
    String target = icon;
    if(target == null) {
      target = file;
    }

    if(target == null) {
      return ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.CYAN);
    }

    ImageKey imageKey = ImageKey.fromString(target, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
    if(imageKey != null) {
      return imageKey;
    }

    return ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.CYAN);
  });

  @Override
  public String getKey() {
    return language;
  }

  @Override
  public Image getInstance() {
    return myIconValue.get();
  }
}
