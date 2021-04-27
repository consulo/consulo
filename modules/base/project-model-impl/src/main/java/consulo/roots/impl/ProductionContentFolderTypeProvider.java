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
import com.intellij.openapi.project.ProjectBundle;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.util.LightDarkColorValue;
import consulo.ui.image.Image;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:37/31.10.13
 */
public class ProductionContentFolderTypeProvider extends ContentFolderTypeProvider {
  private static final ColorValue SOURCES_COLOR = new LightDarkColorValue(new RGBColor(10, 80, 161), StandardColors.BLUE);

  @Nonnull
  public static ProductionContentFolderTypeProvider getInstance() {
    return EP_NAME.findExtensionOrFail(ProductionContentFolderTypeProvider.class);
  }

  public ProductionContentFolderTypeProvider() {
    super("PRODUCTION");
  }

  @Override
  public int getWeight() {
    return 50;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Modules.SourceRoot;
  }

  @Override
  public Image getChildPackageIcon() {
    return AllIcons.Nodes.Package;
  }

  @Nonnull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.sources.action");
  }

  @Nonnull
  @Override
  public ColorValue getGroupColor() {
    return SOURCES_COLOR;
  }
}
