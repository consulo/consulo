// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ide.impl.idea.util.ui.FilePathSplittingPolicy;
import consulo.ui.ex.JBColor;
import consulo.component.util.Iconable;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFileSystemItem;
import consulo.module.content.ProjectFileIndex;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Objects;

public class GotoFileCellRenderer extends PsiElementListCellRenderer<PsiFileSystemItem> {
  private final int myMaxWidth;

  public GotoFileCellRenderer(int maxSize) {
    myMaxWidth = maxSize;
  }

  @Override
  public String getElementText(PsiFileSystemItem element) {
    return element.getName();
  }

  @Override
  protected String getContainerText(PsiFileSystemItem element, String name) {
    PsiFileSystemItem parent = element.getParent();
    final PsiDirectory psiDirectory = parent instanceof PsiDirectory ? (PsiDirectory)parent : null;
    if (psiDirectory == null) return null;
    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    final String relativePath = getRelativePath(virtualFile, element.getProject());
    if (relativePath == null) return "( " + File.separator + " )";
    String path = FilePathSplittingPolicy.SPLIT_BY_SEPARATOR.getOptimalTextForComponent(name + "          ", new File(relativePath), this, myMaxWidth);
    return "(" + path + ")";
  }

  @Nullable
  public static String getRelativePath(final VirtualFile virtualFile, final Project project) {
    if (project == null) {
      return virtualFile.getPresentableUrl();
    }
    VirtualFile root = getAnyRoot(virtualFile, project);
    if (root != null) {
      return getRelativePathFromRoot(virtualFile, root);
    }

    String url = virtualFile.getPresentableUrl();
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir != null) {
      final String projectHomeUrl = baseDir.getPresentableUrl();
      if (url.startsWith(projectHomeUrl)) {
        final String cont = url.substring(projectHomeUrl.length());
        if (cont.isEmpty()) return null;
        url = "..." + cont;
      }
    }
    return url;
  }

  @Nullable
  public static VirtualFile getAnyRoot(@Nonnull VirtualFile virtualFile, @Nonnull Project project) {
    ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
    VirtualFile root = index.getContentRootForFile(virtualFile);
    if (root == null) root = index.getClassRootForFile(virtualFile);
    if (root == null) root = index.getSourceRootForFile(virtualFile);
    return root;
  }

  @Nonnull
  static String getRelativePathFromRoot(@Nonnull VirtualFile file, @Nonnull VirtualFile root) {
    return root.getName() + File.separatorChar + VfsUtilCore.getRelativePath(file, root, File.separatorChar);
  }

  @Override
  protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer, JList list, Object value, int index, boolean selected, boolean hasFocus) {
    return doCustomizeNonPsiElementLeftRenderer(renderer, list, value, getNavigationItemAttributes(value));
  }

  public static boolean doCustomizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer, JList list, Object value, TextAttributes attributes) {
    if (!(value instanceof NavigationItem)) return false;

    NavigationItem item = (NavigationItem)value;

    SimpleTextAttributes nameAttributes = attributes != null ? TextAttributesUtil.fromTextAttributes(attributes) : null;

    Color color = list.getForeground();
    if (nameAttributes == null) nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);

    ItemPresentation presentation = Objects.requireNonNull(item.getPresentation());
    renderer.append(presentation.getPresentableText() + " ", nameAttributes);
    renderer.setIcon(presentation.getIcon(true));

    String locationString = presentation.getLocationString();
    if (!StringUtil.isEmpty(locationString)) {
      renderer.append(locationString, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
    }
    return true;
  }

  @Override
  protected int getIconFlags() {
    return Iconable.ICON_FLAG_READ_STATUS;
  }
}
