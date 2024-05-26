/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util;

import consulo.annotation.DeprecationInfo;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.util.ui.JBImageIcon;
import consulo.ui.ex.awt.ImageUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("deprecation")
public class IconUtil {

  public static int getDefaultNodeIconSize() {
    return consulo.ui.image.Image.DEFAULT_ICON_SIZE;
  }

  public static consulo.ui.image.Image getAddIcon() {
    return AllIcons.General.Add;
  }

  public static consulo.ui.image.Image getRemoveIcon() {
    return AllIcons.General.Remove;
  }

  public static consulo.ui.image.Image getMoveUpIcon() {
    return AllIcons.Actions.MoveUp;
  }

  public static consulo.ui.image.Image getMoveDownIcon() {
    return AllIcons.Actions.MoveDown;
  }

  public static consulo.ui.image.Image getEditIcon() {
    return AllIcons.Actions.Edit;
  }

  public static consulo.ui.image.Image getAddClassIcon() {
    return AllIcons.ToolbarDecorator.AddClass;
  }

  public static consulo.ui.image.Image getAddPatternIcon() {
    return AllIcons.ToolbarDecorator.AddPattern;
  }

  @Deprecated
  @DeprecationInfo("Use task icons")
  public static consulo.ui.image.Image getAddJiraPatternIcon() {
    return AllIcons.ToolbarDecorator.AddJira;
  }

  @Deprecated
  @DeprecationInfo("Use task icons")
  public static consulo.ui.image.Image getAddYouTrackPatternIcon() {
    return AllIcons.ToolbarDecorator.AddYouTrack;
  }

  public static consulo.ui.image.Image getAddBlankLineIcon() {
    return AllIcons.ToolbarDecorator.AddBlankLine;
  }

  public static consulo.ui.image.Image getAddPackageIcon() {
    return AllIcons.ToolbarDecorator.AddPackage;
  }

  public static consulo.ui.image.Image getAddLinkIcon() {
    return AllIcons.ToolbarDecorator.AddLink;
  }

  public static consulo.ui.image.Image getAddFolderIcon() {
    return AllIcons.ToolbarDecorator.AddFolder;
  }

  @Nonnull
  public static JBImageIcon createImageIcon(@Nonnull final Image img) {
    return new JBImageIcon(img) {
      @Override
      public int getIconWidth() {
        return ImageUtil.getUserWidth(getImage());
      }

      @Override
      public int getIconHeight() {
        return ImageUtil.getUserHeight(getImage());
      }
    };
  }

}
