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

import com.intellij.lang.LanguageExtension;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 12:27/08.10.13
 */
public class LanguageElementIcons extends LanguageExtension<Image> {
  public static final LanguageElementIcons INSTANCE = new LanguageElementIcons();

  public LanguageElementIcons() {
    super("com.intellij.lang.elementIcon");
  }
}
