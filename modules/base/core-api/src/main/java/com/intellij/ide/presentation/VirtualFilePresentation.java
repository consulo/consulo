package com.intellij.ide.presentation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.TypePresentationService;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class VirtualFilePresentation {
  @Nonnull
  public static Image getIcon(@Nonnull VirtualFile vFile) {
    Image icon = TypePresentationService.getInstance().getIcon(vFile);
    if (icon != null) {
      return icon;
    }
    if (vFile.isDirectory())  {
      return AllIcons.Nodes.Folder;
    }
    return vFile.getFileType().getIcon();
  }
}
