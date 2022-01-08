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
package com.intellij.usages.impl.rules;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.UsageGroupingRule;
import com.intellij.usages.rules.UsageInFile;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public class NonCodeUsageGroupingRule implements UsageGroupingRule {
  private final Project myProject;

  public NonCodeUsageGroupingRule(Project project) {
    myProject = project;
  }

  private static class CodeUsageGroup extends UsageGroupBase {
    private static final UsageGroup INSTANCE = new CodeUsageGroup();

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return view == null ? UsageViewBundle.message("node.group.code.usages") : view.getPresentation().getCodeUsagesString();
    }

    @Override
    public String toString() {
      //noinspection HardCodedStringLiteral
      return "CodeUsages";
    }

    @Override
    public int compareTo(@Nonnull UsageGroup usageGroup) {
      if (usageGroup instanceof DynamicUsageGroup) {
        return -1;
      }
      return usageGroup == this ? 0 : 1;
    }
  }

  private static class UsageInGeneratedCodeGroup extends UsageGroupBase {
    public static final UsageGroup INSTANCE = new UsageInGeneratedCodeGroup();

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return view == null ? UsageViewBundle.message("node.usages.in.generated.code") : view.getPresentation().getUsagesInGeneratedCodeString();
    }

    @Override
    public String toString() {
      return "UsagesInGeneratedCode";
    }

    @Override
    public int compareTo(@Nonnull UsageGroup usageGroup) {
      return usageGroup == this ? 0 : -1;
    }
  }

  private static class NonCodeUsageGroup extends UsageGroupBase {
    public static final UsageGroup INSTANCE = new NonCodeUsageGroup();

    @Override
    @Nonnull
    public String getText(UsageView view) {
      return view == null ? UsageViewBundle.message("node.group.code.usages") : view.getPresentation().getNonCodeUsagesString();
    }

    @Override
    public void update() {
    }

    @Override
    public String toString() {
      //noinspection HardCodedStringLiteral
      return "NonCodeUsages";
    }

    @Override
    public int compareTo(@Nonnull UsageGroup usageGroup) {
      return usageGroup == this ? 0 : -1;
    }
  }

  private static class DynamicUsageGroup extends UsageGroupBase {
    public static final UsageGroup INSTANCE = new DynamicUsageGroup();
    @NonNls private static final String DYNAMIC_CAPTION = "Dynamic usages";

    @Override
    @Nonnull
    public String getText(UsageView view) {
      if (view == null) {
        return DYNAMIC_CAPTION;
      }
      else {
        final String dynamicCodeUsagesString = view.getPresentation().getDynamicCodeUsagesString();
        return dynamicCodeUsagesString == null ? DYNAMIC_CAPTION : dynamicCodeUsagesString;
      }
    }

    @Override
    public String toString() {
      //noinspection HardCodedStringLiteral
      return "DynamicUsages";
    }

    @Override
    public int compareTo(@Nonnull UsageGroup usageGroup) {
      return usageGroup == this ? 0 : 1;
    }
  }

  @Override
  public UsageGroup groupUsage(@Nonnull Usage usage) {
    if (usage instanceof UsageInFile) {
      VirtualFile file = ((UsageInFile)usage).getFile();
      if (file != null) {
        if (GeneratedSourcesFilter.isGenerated(myProject, file)) {
          return UsageInGeneratedCodeGroup.INSTANCE;
        }
      }
    }
    if (usage instanceof PsiElementUsage) {
      if (usage instanceof UsageInfo2UsageAdapter) {
        final UsageInfo usageInfo = ((UsageInfo2UsageAdapter)usage).getUsageInfo();
        if (usageInfo.isDynamicUsage()) {
          return DynamicUsageGroup.INSTANCE;
        }
      }
      if (((PsiElementUsage)usage).isNonCodeUsage()) {
        return NonCodeUsageGroup.INSTANCE;
      }
      else {
        return CodeUsageGroup.INSTANCE;
      }
    }
    return null;
  }
}
