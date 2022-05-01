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
package consulo.ide.impl.idea.usages.impl.rules;

import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.fileEditor.FileEditorManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.component.util.Iconable;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.usage.rule.SingleParentUsageGroupingRule;
import consulo.usage.rule.UsageInFile;
import consulo.ide.impl.VfsIconUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

  protected static class FileUsageGroup implements UsageGroup, TypeSafeDataProvider, NamedPresentably {
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
      return VfsIconUtil.getIcon(myFile, Iconable.ICON_FLAG_READ_STATUS, myProject);
    }

    @Override
    public void update() {
      if (isValid()) {
        myIcon = getIconImpl();
        myPresentableName = myFile.getName();
      }
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof FileUsageGroup)) return false;

      final FileUsageGroup fileUsageGroup = (FileUsageGroup)o;

      return myFile.equals(fileUsageGroup.myFile);
    }

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
    public void calcData(final Key<?> key, final DataSink sink) {
      if (!isValid()) return;
      if (key == CommonDataKeys.VIRTUAL_FILE) {
        sink.put(CommonDataKeys.VIRTUAL_FILE, myFile);
      }
      if (key == CommonDataKeys.PSI_ELEMENT) {
        sink.put(CommonDataKeys.PSI_ELEMENT, getPsiFile());
      }
    }

    @Nullable
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
