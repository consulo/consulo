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
package consulo.ide.impl.idea.usages.impl.rules;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageView;
import consulo.usage.rule.UsageGroupingRule;
import consulo.usage.rule.UsageInFile;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class DirectoryGroupingRule implements UsageGroupingRule {
  protected final Project myProject;

  public DirectoryGroupingRule(Project project) {
    myProject = project;
  }

  @Override
  @Nullable
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

  @RequiredReadAction
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

    @RequiredReadAction
    private PsiDirectory getDirectory() {
      return myDir.isValid() ? PsiManager.getInstance(myProject).findDirectory(myDir) : null;
    }

    @Override
    @RequiredReadAction
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
      return o instanceof DirectoryGroup directoryGroup && myDir.equals(directoryGroup.myDir);
    }

    @Override
    public int hashCode() {
      return myDir.hashCode();
    }

    @Override
    public void calcData(final Key<?> key, final DataSink sink) {
      if (!isValid()) return;
      if (VirtualFile.KEY == key) {
        sink.put(VirtualFile.KEY, myDir);
      }
      if (PsiElement.KEY == key) {
        sink.put(PsiElement.KEY, getDirectory());
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
      return o instanceof PackageGroup packageGroup && myPackage.equals(packageGroup.myPackage);
    }

    @Override
    public int hashCode() {
      return myPackage.hashCode();
    }

    @Override
    public void calcData(final Key<?> key, final DataSink sink) {
      if (!isValid()) return;
      if (PsiElement.KEY == key) {
        sink.put(PsiElement.KEY, myPackage);
      }
    }
  }
}
