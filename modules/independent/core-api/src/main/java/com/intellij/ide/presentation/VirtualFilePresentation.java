package com.intellij.ide.presentation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.TypePresentationService;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.awt.TargetAWT;

import javax.swing.*;

/**
 * @author yole
 */
public class VirtualFilePresentation {
  public static Icon getIcon(VirtualFile vFile) {
    Icon icon = TypePresentationService.getInstance().getIcon(vFile);
    if (icon != null) {
      return icon;
    }
    if (vFile.isDirectory() && vFile.isInLocalFileSystem()) {
      return AllIcons.Nodes.Folder;
    }
    return TargetAWT.to(vFile.getFileType().getIcon());
  }
}
