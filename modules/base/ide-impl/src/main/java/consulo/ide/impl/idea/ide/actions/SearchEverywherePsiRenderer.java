// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.util.SystemInfo;
import consulo.application.util.UserHomeFileUtil;
import consulo.component.util.Iconable;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileCellRenderer;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.*;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.Optional;

/**
 * @author Konstantin Bulenkov
 */
public class SearchEverywherePsiRenderer extends PsiElementListCellRenderer<PsiElement> {

  public SearchEverywherePsiRenderer() {
    setLayout(new BorderLayout() {
      @Override
      public void layoutContainer(Container target) {
        super.layoutContainer(target);
        final Component right = getLayoutComponent(EAST);
        final Component left = getLayoutComponent(WEST);

        //IDEA-140824
        if (right != null && left != null && left.getBounds().x + left.getBounds().width > right.getBounds().x) {
          final Rectangle bounds = right.getBounds();
          final int newX = left.getBounds().x + left.getBounds().width;
          right.setBounds(newX, bounds.y, bounds.width - (newX - bounds.x), bounds.height);
        }
      }
    });
  }

  @Override
  public String getElementText(PsiElement element) {
    VirtualFile file = element instanceof PsiFile ? PsiUtilCore.getVirtualFile(element) : element instanceof VirtualFile ? (VirtualFile)element : null;
    if (file != null) {
      return VfsPresentationUtil.getPresentableNameForUI(element.getProject(), file);
    }
    String name = element instanceof PsiNamedElement ? ((PsiNamedElement)element).getName() : null;
    return StringUtil.notNullize(name, "<unnamed>");
  }

  @Nullable
  @Override
  protected String getContainerText(PsiElement element, String name) {
    return getContainerTextForLeftComponent(element, name, -1, null);
  }

  @Nullable
  @Override
  protected String getContainerTextForLeftComponent(PsiElement element, String name, int maxWidth, FontMetrics fm) {
    String text = SymbolPresentationUtil.getSymbolContainerText(element);

    if (text == null) return null;

    if (text.startsWith("(") && text.endsWith(")")) {
      text = text.substring(1, text.length() - 1);
    }

    if ((text.contains("/") || text.contains(File.separator)) && element instanceof PsiFileSystemItem) {
      Project project = element.getProject();
      String basePath = Optional.ofNullable(project.getBasePath()).map(FileUtil::toSystemDependentName).orElse(null);
      VirtualFile file = ((PsiFileSystemItem)element).getVirtualFile();
      if (file != null) {
        text = FileUtil.toSystemDependentName(text);
        String filePath = FileUtil.toSystemDependentName(file.getPath());
        if (basePath != null && FileUtil.isAncestor(basePath, filePath, true)) {
          text = ObjectUtils.notNull(FileUtil.getRelativePath(basePath, text, File.separatorChar), text);
        }
        else {
          String rootPath = Optional.ofNullable(GotoFileCellRenderer.getAnyRoot(file, project)).map(root -> FileUtil.toSystemDependentName(root.getPath()))
                  .filter(root -> basePath != null && FileUtil.isAncestor(basePath, root, true)).orElse(null);
          text = rootPath != null ? ObjectUtils.notNull(FileUtil.getRelativePath(rootPath, text, File.separatorChar), text) : UserHomeFileUtil.getLocationRelativeToUserHome(text);
        }
      }
    }

    boolean in = text.startsWith("in ");
    if (in) text = text.substring(3);
    String left = in ? "in " : "";
    String adjustedText = left + text;
    if (maxWidth < 0) return adjustedText;

    int fullWidth = fm.stringWidth(adjustedText);
    if (fullWidth < maxWidth) return adjustedText;
    String separator = text.contains("/") ? "/" : SystemInfo.isWindows && text.contains("\\") ? "\\" : text.contains(".") ? "." : text.contains("-") ? "-" : " ";
    LinkedList<String> parts = new LinkedList<>(StringUtil.split(text, separator));
    int index;
    while (parts.size() > 1) {
      index = parts.size() / 2 - 1;
      parts.remove(index);
      if (fm.stringWidth(StringUtil.join(parts, separator) + "...") < maxWidth) {
        parts.add(index, "...");
        return left + StringUtil.join(parts, separator);
      }
    }
    int adjustedWidth = Math.max(adjustedText.length() * maxWidth / fullWidth - 1, left.length() + 3);
    return StringUtil.trimMiddle(adjustedText, adjustedWidth);
  }


  @Override
  protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer, JList list, Object value, int index, boolean selected, boolean hasFocus) {
    return GotoFileCellRenderer.doCustomizeNonPsiElementLeftRenderer(renderer, list, value, getNavigationItemAttributes(value));
  }

  @Override
  protected int getIconFlags() {
    return Iconable.ICON_FLAG_READ_STATUS;
  }
}
