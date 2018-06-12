/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconUtilEx;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.http.HttpFileSystem;
import consulo.ui.image.Image;
import consulo.vfs.ArchiveFileSystem;

import javax.annotation.Nonnull;
import java.io.File;


class ItemElement extends LibraryTableTreeContentElement<ItemElement> {
  protected final String myUrl;
  private final OrderRootType myRootType;

  public ItemElement(@Nonnull OrderRootTypeElement parent, @Nonnull String url, @Nonnull OrderRootType rootType, final boolean isJarDirectory,
                     boolean isValid) {
    super(parent);
    myUrl = url;
    myName = getPresentablePath(url).replace('/', File.separatorChar);
    myColor = getForegroundColor(isValid);
    setIcon(getIconForUrl(url, isValid, isJarDirectory));
    myRootType = rootType;
  }

  private static Image getIconForUrl(final String url, final boolean isValid, final boolean isJarDirectory) {
    final Image icon;
    if (isValid) {
      VirtualFile presentableFile;
      if (isArchiveFileRoot(url)) {
        presentableFile = LocalFileSystem.getInstance().findFileByPath(getPresentablePath(url));
      }
      else {
        presentableFile = VirtualFileManager.getInstance().findFileByUrl(url);
      }
      if (presentableFile != null && presentableFile.isValid()) {
        if (presentableFile.getFileSystem() instanceof HttpFileSystem) {
          icon = AllIcons.Nodes.PpWeb;
        }
        else {
          if (presentableFile.isDirectory()) {
            if (isJarDirectory) {
              icon = AllIcons.Nodes.JarDirectory;
            }
            else {
              icon = AllIcons.Nodes.TreeClosed;
            }
          }
          else {
            icon = IconUtilEx.getIcon(presentableFile, 0, null);
          }
        }
      }
      else {
        icon = AllIcons.Nodes.PpInvalid;
      }
    }
    else {
      icon = AllIcons.Nodes.PpInvalid;
    }
    return icon;
  }

  public static String getPresentablePath(final String url) {
    String presentablePath = VirtualFileManager.extractPath(url);
    if (isArchiveFileRoot(url)) {
      presentablePath = presentablePath.substring(0, presentablePath.length() - ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
    }
    return presentablePath;
  }

  private static boolean isArchiveFileRoot(final String url) {
    return VirtualFileManager.extractPath(url).endsWith(ArchiveFileSystem.ARCHIVE_SEPARATOR);
  }

  public OrderRootTypeElement getParent() {
    return (OrderRootTypeElement)getParentDescriptor();
  }

  @Nonnull
  public OrderRootType getRootType() {
    return myRootType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ItemElement)) return false;

    final ItemElement itemElement = (ItemElement)o;

    if (!getParent().equals(itemElement.getParent())) return false;
    if (!myRootType.equals(itemElement.myRootType)) return false;
    if (!myUrl.equals(itemElement.myUrl)) return false;

    return true;
  }

  @Nonnull
  public String getUrl() {
    return myUrl;
  }

  @Override
  public int hashCode() {
    int result;
    result = getParent().hashCode();
    result = 29 * result + myUrl.hashCode();
    result = 29 * result + myRootType.hashCode();
    return result;
  }
}
