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
package com.intellij.ide.projectView.impl.nodes;

import consulo.application.CommonBundle;
import consulo.ui.ex.tree.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import consulo.codeEditor.CodeInsightColors;
import consulo.project.Project;
import consulo.module.content.layer.orderEntry.OrderEntry;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Comparing;
import consulo.component.util.Iconable;
import com.intellij.openapi.util.io.FileUtil;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class PsiFileNode extends BasePsiNode<PsiFile> implements NavigatableWithText {
  public PsiFileNode(Project project, @Nonnull PsiFile value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    Project project = getProject();
    VirtualFile jarRoot = getArchiveRoot();
    if (project != null && jarRoot != null) {
      PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(jarRoot);
      if (psiDirectory != null) {
        return BaseProjectViewDirectoryHelper.getDirectoryChildren(psiDirectory, getSettings(), true);
      }
    }

    return ContainerUtil.emptyList();
  }

  private boolean isArchive() {
    VirtualFile file = getVirtualFile();
    return file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    PsiFile value = getValue();
    data.setPresentableText(value.getName());
    data.setIcon(IconDescriptorUpdaters.getIcon(value, Iconable.ICON_FLAG_READ_STATUS));

    VirtualFile file = getVirtualFile();
    if (file != null && file.is(VFileProperty.SYMLINK)) {
      String target = file.getCanonicalPath();
      if (target == null) {
        data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
        data.setTooltip(CommonBundle.message("vfs.broken.link"));
      }
      else {
        data.setTooltip(FileUtil.toSystemDependentName(target));
      }
    }
  }

  @Override
  public boolean canNavigate() {
    return isNavigatableLibraryRoot() || super.canNavigate();
  }

  private boolean isNavigatableLibraryRoot() {
    VirtualFile jarRoot = getArchiveRoot();
    final Project project = getProject();
    if (jarRoot != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
      final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(jarRoot, project);
      return orderEntry != null ;
    }
    return false;
  }

  @Nullable
  private VirtualFile getArchiveRoot() {
    final VirtualFile file = getVirtualFile();

    return ArchiveVfsUtil.getArchiveRootForLocalFile(file);
  }

  @Override
  public void navigate(boolean requestFocus) {
    final VirtualFile jarRoot = getArchiveRoot();
    final Project project = getProject();
    if (requestFocus && jarRoot != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
      final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(jarRoot, project);
      if (orderEntry != null) {
        ProjectSettingsService.getInstance(project).openLibraryOrSdkSettings(orderEntry);
        return;
      }
    }

    super.navigate(requestFocus);
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    if (isNavigatableLibraryRoot()) {
      return "Open Library Settings";
    }
    return null;
  }

  @Override
  public int getWeight() {
    return 20;
  }

  @Override
  public String getTitle() {
    VirtualFile file = getVirtualFile();
    if (file != null) {
      return FileUtil.getLocationRelativeToUserHome(file.getPresentableUrl());
    }

    return super.getTitle();
  }

  @Override
  protected boolean isMarkReadOnly() {
    return true;
  }

  @Override
  public Comparable getTypeSortKey() {
    String extension = extension(getValue());
    return extension == null ? null : new ExtensionSortKey(extension);
  }

  @Nullable
  public static String extension(@Nullable PsiFile file) {
    if (file != null) {
      VirtualFile vFile = file.getVirtualFile();
      if (vFile != null) {
        return vFile.getFileType().getDefaultExtension();
      }
    }

    return null;
  }

  public static class ExtensionSortKey implements Comparable {
    private final String myExtension;

    public ExtensionSortKey(final String extension) {
      myExtension = extension;
    }

    @Override
    public int compareTo(final Object o) {
      if (!(o instanceof ExtensionSortKey)) return 0;
      ExtensionSortKey rhs = (ExtensionSortKey) o;
      return myExtension.compareTo(rhs.myExtension);
    }
  }

  @Override
  public boolean shouldDrillDownOnEmptyElement() {
    return true;
  }

  @Override
  public boolean canRepresent(final Object element) {
    return super.canRepresent(element) || getValue() != null && getValue().getVirtualFile() == element;
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    return super.contains(file) || isArchive() && Comparing.equal(VirtualFilePathUtil.getLocalFile(file), getVirtualFile());
  }
}
