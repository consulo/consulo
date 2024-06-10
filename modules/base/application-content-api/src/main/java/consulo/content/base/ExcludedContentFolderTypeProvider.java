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
package consulo.content.base;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionInstance;
import consulo.content.ContentFolderTypeProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import consulo.ui.style.StandardColors;
import consulo.ui.util.LightDarkColorValue;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 22:46/31.10.13
 */
@ExtensionImpl
public final class ExcludedContentFolderTypeProvider extends ContentFolderTypeProvider {
  private static final Supplier<ExcludedContentFolderTypeProvider> INSTANCE =
    ExtensionInstance.from(ContentFolderTypeProvider.class);

  private static final ColorValue EXCLUDED_COLOR =
    new LightDarkColorValue(new RGBColor(153, 46, 0), StandardColors.RED);

  @Nonnull
  public static ExcludedContentFolderTypeProvider getInstance() {
    return INSTANCE.get();
  }

  public ExcludedContentFolderTypeProvider() {
    super("EXCLUDED");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.modulesExcluderoot();
  }

  @Nonnull
  @Override
  public Image getChildDirectoryIcon() {
    return PlatformIconGroup.modulesExcluderoot();
  }

  @Nonnull
  @Override
  public String getName() {
    return ProjectLocalize.moduleToggleExcludedAction().get();
  }

  @Nonnull
  @Override
  public ColorValue getGroupColor() {
    return EXCLUDED_COLOR;
  }
}
