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
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07.11.13.
 */
public class WebResourcesFolderTypeProvider extends ContentFolderTypeProvider {
  @Nonnull
  public static ContentFolderTypeProvider getInstance() {
    return EP_NAME.findExtension(WebResourcesFolderTypeProvider.class);
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

  @Override
  public Image getChildDirectoryIcon() {
    return AllIcons.Nodes.WebFolder;
  }

  @Nonnull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.web.resources.action");
  }

  @Nonnull
  @Override
  public consulo.ui.color.ColorValue getGroupColor() {
    return ProductionResourceContentFolderTypeProvider.getInstance().getGroupColor();
  }
}
