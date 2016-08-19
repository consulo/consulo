/*
 * Copyright 2013 must-be.org
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
import consulo.lombok.annotations.Lazy;
import org.jetbrains.annotations.NotNull;
import consulo.roots.ContentFolderTypeProvider;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 22:46/31.10.13
 */
public class ExcludedContentFolderTypeProvider extends ContentFolderTypeProvider {
  private static final Color EXCLUDED_COLOR = new JBColor(new Color(0x992E00), DarculaColors.RED);

  @NotNull
  @Lazy
  public static ExcludedContentFolderTypeProvider getInstance() {
    return EP_NAME.findExtension(ExcludedContentFolderTypeProvider.class);
  }

  public ExcludedContentFolderTypeProvider() {
    super("EXCLUDED");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Modules.ExcludeRoot;
  }

  @Override
  public Icon getChildDirectoryIcon() {
    return AllIcons.Modules.ExcludeRoot;
  }

  @NotNull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.excluded.action");
  }

  @NotNull
  @Override
  public Color getGroupColor() {
    return EXCLUDED_COLOR;
  }
}
