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
package com.intellij.usages.impl.rules;

import com.intellij.icons.AllIcons;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.TypeSafeDataProvider;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageView;
import com.intellij.usages.rules.UsageGroupingRule;
import com.intellij.usages.rules.UsageInFile;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class DirectoryGroupingRule implements UsageGroupingRule {
  protected final Project myProject;

  public DirectoryGroupingRule(Project project) {
    myProject = project;
  }

  @Override
  @javax.annotation.Nullable
  public UsageGroup groupUsage(@Nonnull Usage usage) {
    if (usage instanceof UsageInFile) {
      UsageInFile usageInFile = (UsageInFile)usage;
      VirtualFile file = usageInFile.getFile();
      if (file != null) {
        if (file instanceof VirtualFileWindow) {
          file = ((VirtualFileWindow)file).getDelegate();
        }
        VirtualFile dir = file.getParent();
        if (dir == null) return null;
        return getGroupForFile(dir);
      }
    }
    return null;
  }

  protected UsageGroup getGroupForFile(final VirtualFile dir) {
    PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(dir);
    if (psiDirectory != null) {
      PsiPackage aPackage = PsiPackageManager.getInstance(myProject).findAnyPackage(psiDirectory);
      if (aPackage != null) {
        return new PackageGroup(aPackage);
      }
    }
    return new DirectoryGroup(dir);
  }

  private class DirectoryGroup implements UsageGroup, TypeSafeDataProvider {
    private final VirtualFile myDir;

    @Override
    public void update() {
    }

    private DirectoryGroup(VirtualFile dir) {
      myDir = dir;
    }

    @Override
    public Image getIcon() {
      return AllIcons.Nodes.TreeClosed;
    }

    @Override
    @Nonnull
    public String getText(UsageView view) {
      String url = myDir.getPresentableUrl();
      return url != null ? url : "<invalid>";
    }

    @Override
    public FileStatus getFileStatus() {
      return isValid() ? FileStatusManager.getInstance(myProject).getStatus(myDir) : null;
    }

    @Override
    public boolean isValid() {
      return myDir.isValid();
    }

    @Override
    public void navigate(boolean focus) throws UnsupportedOperationException {
      final PsiDirectory directory = getDirectory();
      if (directory != null && directory.canNavigate()) {
        directory.navigate(focus);
      }
    }

    private PsiDirectory getDirectory() {
      return myDir.isValid() ? PsiManager.getInstance(myProject).findDirectory(myDir) : null;
    }

    @Override
    public boolean canNavigate() {
      final PsiDirectory directory = getDirectory();
      return directory != null && directory.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public int compareTo(UsageGroup usageGroup) {
      return getText(null).compareToIgnoreCase(usageGroup.getText(null));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof DirectoryGroup)) return false;
      return myDir.equals(((DirectoryGroup)o).myDir);
    }

    @Override
    public int hashCode() {
      return myDir.hashCode();
    }

    @Override
    public void calcData(final Key<?> key, final DataSink sink) {
      if (!isValid()) return;
      if (PlatformDataKeys.VIRTUAL_FILE == key) {
        sink.put(PlatformDataKeys.VIRTUAL_FILE, myDir);
      }
      if (LangDataKeys.PSI_ELEMENT == key) {
        sink.put(LangDataKeys.PSI_ELEMENT, getDirectory());
      }
    }
  }

  private class PackageGroup implements UsageGroup, TypeSafeDataProvider {
    private final PsiPackage myPackage;

    private PackageGroup(PsiPackage aPackage) {
      myPackage = aPackage;
      update();
    }

    @Override
    public void update() {

    }

    @Override
    public Image getIcon() {
      return AllIcons.Nodes.Package;
    }

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return myPackage.getQualifiedName();
    }

    @Override
    public FileStatus getFileStatus() {
      if (!isValid()) return null;
      PsiDirectory[] dirs = myPackage.getDirectories();
      return dirs.length == 1 ? FileStatusManager.getInstance(myProject).getStatus(dirs[0].getVirtualFile()) : null;
    }

    @Override
    public boolean isValid() {
      return myPackage.isValid();
    }

    @Override
    public void navigate(boolean focus) throws UnsupportedOperationException {
      myPackage.navigate(focus);
    }

    @Override
    public boolean canNavigate() {
      return myPackage.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public int compareTo(UsageGroup usageGroup) {
      return getText(null).compareToIgnoreCase(usageGroup.getText(null));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PackageGroup)) return false;

      return myPackage.equals(((PackageGroup)o).myPackage);
    }

    @Override
    public int hashCode() {
      return myPackage.hashCode();
    }

    @Override
    public void calcData(final Key<?> key, final DataSink sink) {
      if (!isValid()) return;
      if (LangDataKeys.PSI_ELEMENT == key) {
        sink.put(LangDataKeys.PSI_ELEMENT, myPackage);
      }
    }
  }
}
