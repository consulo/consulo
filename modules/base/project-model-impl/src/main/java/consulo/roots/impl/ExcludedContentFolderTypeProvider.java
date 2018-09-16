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
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:46/31.10.13
 */
public class ExcludedContentFolderTypeProvider extends ContentFolderTypeProvider {
  private static final java.awt.Color EXCLUDED_COLOR = new JBColor(new java.awt.Color(0x992E00), DarculaColors.RED);

  @Nonnull
  public static ExcludedContentFolderTypeProvider getInstance() {
    return EP_NAME.findExtension(ExcludedContentFolderTypeProvider.class);
  }

  public ExcludedContentFolderTypeProvider() {
    super("EXCLUDED");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Modules.ExcludeRoot;
  }

  @Override
  public Image getChildDirectoryIcon() {
    return AllIcons.Modules.ExcludeRoot;
  }

  @Nonnull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.excluded.action");
  }

  @Nonnull
  @Override
  public java.awt.Color getGroupColor() {
    return EXCLUDED_COLOR;
  }
}
