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
package consulo.lang;

import com.intellij.openapi.extensions.CustomLoadingExtensionPointBean;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 12:27/08.10.13
 */
public class LanguageElementIcon extends CustomLoadingExtensionPointBean implements KeyedLazyInstance<Image> {

  // these must be public for scrambling compatibility
  @Attribute("language")
  public String language;

  @Attribute("file")
  public String file;

  private NullableLazyValue<Image> myIconValue = NullableLazyValue.<Image>of(() -> IconLoader.findIcon(file, getLoaderForClass()));

  @Override
  public String getKey() {
    return language;
  }

  @Override
  public Image getInstance() {
    return myIconValue.getValue();
  }
}
