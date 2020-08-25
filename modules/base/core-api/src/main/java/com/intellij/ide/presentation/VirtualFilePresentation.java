package com.intellij.ide.presentation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.TypePresentationService;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author yole
 */
public class VirtualFilePresentation {
  @Nonnull
  @Deprecated
  public static Icon getAWTIcon(VirtualFile vFile) {
    return TargetAWT.to(getIcon(vFile));
  }

  @Nonnull
  public static Image getIcon(VirtualFile vFile) {
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
