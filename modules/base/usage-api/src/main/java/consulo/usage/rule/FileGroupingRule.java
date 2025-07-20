/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.usage.rule;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.component.util.Iconable;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.fileEditor.FileEditorManager;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class FileGroupingRule extends SingleParentUsageGroupingRule implements DumbAware {
  private final Project myProject;

  public FileGroupingRule(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public UsageGroup getParentGroupFor(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
    VirtualFile virtualFile;
    if (usage instanceof UsageInFile && (virtualFile = ((UsageInFile)usage).getFile()) != null) {
      return new FileUsageGroup(myProject, virtualFile);
    }
    return null;
  }

  public static class FileUsageGroup implements UsageGroup, TypeSafeDataProvider, NamedPresentably {
    private final Project myProject;
    private final VirtualFile myFile;
    private String myPresentableName;
    private Image myIcon;

    public FileUsageGroup(@Nonnull Project project, @Nonnull VirtualFile file) {
      myProject = project;
      myFile = file instanceof VirtualFileWindow ? ((VirtualFileWindow)file).getDelegate() : file;
      myPresentableName = myFile.getName();
      update();
    }

    private Image getIconImpl() {
      return VirtualFileManager.getInstance().getFileIcon(myFile, myProject, Iconable.ICON_FLAG_READ_STATUS);
    }

    @Override
    public void update() {
      if (isValid()) {
        myIcon = getIconImpl();
        myPresentableName = myFile.getName();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof FileUsageGroup)) return false;

      FileUsageGroup fileUsageGroup = (FileUsageGroup)o;

      return myFile.equals(fileUsageGroup.myFile);
    }

    @Override
    public int hashCode() {
      return myFile.hashCode();
    }

    @Override
    public Image getIcon() {
      return myIcon;
    }

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return myPresentableName;
    }

    @Override
    public FileStatus getFileStatus() {
      return isValid() ? FileStatusManager.getInstance(myProject).getStatus(myFile) : null;
    }

    @Override
    public boolean isValid() {
      return myFile.isValid();
    }

    @Override
    public void navigate(boolean focus) throws UnsupportedOperationException {
      FileEditorManager.getInstance(myProject).openFile(myFile, focus);
    }

    @Override
    public boolean canNavigate() {
      return myFile.isValid();
    }

    @Override
    public boolean canNavigateToSource() {
      return canNavigate();
    }

    @Override
    public int compareTo(@Nonnull UsageGroup otherGroup) {
      int compareTexts = getText(null).compareToIgnoreCase(otherGroup.getText(null));
      if (compareTexts != 0) return compareTexts;
      if (otherGroup instanceof FileUsageGroup) {
        return myFile.getPath().compareTo(((FileUsageGroup)otherGroup).myFile.getPath());
      }
      return 0;
    }

    @Override
    @RequiredReadAction
    public void calcData(Key<?> key, DataSink sink) {
      if (!isValid()) return;
      if (key == VirtualFile.KEY) {
        sink.put(VirtualFile.KEY, myFile);
      }
      if (key == PsiElement.KEY) {
        sink.put(PsiElement.KEY, getPsiFile());
      }
    }

    @Nullable
    @RequiredReadAction
    public PsiFile getPsiFile() {
      return myFile.isValid() ? PsiManager.getInstance(myProject).findFile(myFile) : null;
    }

    @Override
    @Nonnull
    public String getPresentableName() {
      return myPresentableName;
    }
  }
}
