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
import consulo.application.AllIcons;
import consulo.component.extension.ExtensionInstance;
import consulo.content.ContentFolderTypeProvider;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * This content folder type allow select directories as web resources.
 * By default modules not allow marking directories for this type. Use {@link ContentFolderSupportPatcher}
 *
 * @author VISTALL
 * @since 07.11.13
 */
@ExtensionImpl
public class WebResourcesFolderTypeProvider extends ContentFolderTypeProvider {
  private static final Supplier<WebResourcesFolderTypeProvider> INSTANCE =
    ExtensionInstance.from(ContentFolderTypeProvider.class);

  @Nonnull
  public static ContentFolderTypeProvider getInstance() {
    return INSTANCE.get();
  }

  public WebResourcesFolderTypeProvider() {
    super("WEB_RESOURCES");
  }

  @Override
  public int getWeight() {
    return 250;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Modules.WebRoot;
  }

  @Nonnull
  @Override
  public Image getChildDirectoryIcon() {
    return AllIcons.Nodes.WebFolder;
  }

  @Nonnull
  @Override
  public String getName() {
    return ProjectLocalize.moduleToggleWebResourcesAction().get();
  }

  @Nonnull
  @Override
  public ColorValue getGroupColor() {
    return new RGBColor(129, 45, 243);
  }
}
