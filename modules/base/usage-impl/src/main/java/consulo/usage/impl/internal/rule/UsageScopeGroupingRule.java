/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.usage.impl.internal.rule;

import consulo.application.AllIcons;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageView;
import consulo.usage.rule.PsiElementUsage;
import consulo.usage.rule.UsageGroupingRule;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class UsageScopeGroupingRule implements UsageGroupingRule {
  @Override
  public UsageGroup groupUsage(@Nonnull Usage usage) {
    if (!(usage instanceof PsiElementUsage)) {
      return null;
    }
    PsiElementUsage elementUsage = (PsiElementUsage)usage;

    PsiElement element = elementUsage.getElement();
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);

    if (virtualFile == null) {
      return null;
    }
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
    boolean isInLib = fileIndex.isInLibraryClasses(virtualFile) || fileIndex.isInLibrarySource(virtualFile);
    if (isInLib) return LIBRARY;
    boolean isInTest = TestSourcesFilter.isTestSources(virtualFile, element.getProject());
    return isInTest ? TEST : PRODUCTION;
  }

  private static final UsageScopeGroup TEST = new UsageScopeGroup(0) {
    @Override
    public Image getIcon() {
      return AllIcons.Nodes.TestPackage;
    }

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return "Test";
    }
  };
  private static final UsageScopeGroup PRODUCTION = new UsageScopeGroup(1) {
    @Override
    public Image getIcon() {
      return AllIcons.Nodes.Package;
    }

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return "Production";
    }
  };
  private static final UsageScopeGroup LIBRARY = new UsageScopeGroup(2) {
    @Override
    public Image getIcon() {
      return AllIcons.Nodes.PpLib;
    }

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return "Library";
    }
  };
  private abstract static class UsageScopeGroup implements UsageGroup {
    private final int myCode;

    private UsageScopeGroup(int code) {
      myCode = code;
    }

    @Override
    public void update() {
    }

    @Override
    public FileStatus getFileStatus() {
      return null;
    }

    @Override
    public boolean isValid() { return true; }
    @Override
    public void navigate(boolean focus) { }
    @Override
    public boolean canNavigate() { return false; }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public int compareTo(UsageGroup usageGroup) {
      return getText(null).compareTo(usageGroup.getText(null));
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof UsageScopeGroup)) return false;
      UsageScopeGroup usageTypeGroup = (UsageScopeGroup)o;
      return myCode == usageTypeGroup.myCode;
    }

    public int hashCode() {
      return myCode;
    }
  }
}
