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
package consulo.roots.impl;

import com.intellij.icons.AllIcons;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:59/31.10.13
 */
public class UnknownContentFolderTypeProvider extends ContentFolderTypeProvider {
  public UnknownContentFolderTypeProvider(String id) {
    super(id);
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Actions.Help;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Unknown";
  }

  @Nonnull
  @Override
  public ColorValue getGroupColor() {
    return StandardColors.DARK_GRAY;
  }
}
