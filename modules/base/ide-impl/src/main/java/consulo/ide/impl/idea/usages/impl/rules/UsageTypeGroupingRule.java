/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.usage.rule.PsiElementUsage;
import consulo.usage.rule.SingleParentUsageGroupingRule;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class UsageTypeGroupingRule extends SingleParentUsageGroupingRule {
    @Nullable
    @Override
    protected UsageGroup getParentGroupFor(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
        if (usage instanceof PsiElementUsage elementUsage) {
            PsiElement element = elementUsage.getElement();
            UsageType usageType = getUsageType(element, targets);

            if (usageType == null && element instanceof PsiFile && elementUsage instanceof UsageInfo2UsageAdapter usageInfo2UsageAdapter) {
                usageType = usageInfo2UsageAdapter.getUsageType();
            }

            if (usageType != null) {
                return new UsageTypeGroup(usageType);
            }

            if (usage instanceof ReadWriteAccessUsage u) {
                if (u.isAccessedForWriting()) {
                    return new UsageTypeGroup(UsageType.WRITE);
                }
                if (u.isAccessedForReading()) {
                    return new UsageTypeGroup(UsageType.READ);
                }
            }

            return new UsageTypeGroup(UsageType.UNCLASSIFIED);
        }

        return null;
    }

    @Nullable
    private static UsageType getUsageType(PsiElement element, @Nonnull UsageTarget[] targets) {
        if (element == null) {
            return null;
        }

        if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) {
            return UsageType.COMMENT_USAGE;
        }

        for (UsageTypeProvider provider : UsageTypeProvider.EP_NAME.getExtensionList()) {
            UsageType usageType = provider.getUsageType(element, targets);

            if (usageType != null) {
                return usageType;
            }
        }

        return null;
    }

    private static class UsageTypeGroup implements UsageGroup {
        private final UsageType myUsageType;

        private UsageTypeGroup(@Nonnull UsageType usageType) {
            myUsageType = usageType;
        }

        @Override
        public void update() {
        }

        @Override
        public Image getIcon() {
            return null;
        }

        @Override
        @Nonnull
        public String getText(@Nullable UsageView view) {
            return view == null ? myUsageType.toString() : myUsageType.toString(view.getPresentation());
        }

        @Override
        public FileStatus getFileStatus() {
            return null;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void navigate(boolean focus) {
        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public int compareTo(@Nonnull UsageGroup usageGroup) {
            return getText(null).compareTo(usageGroup.getText(null));
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof UsageTypeGroup usageTypeGroup
                && myUsageType.equals(usageTypeGroup.myUsageType);
        }

        @Override
        public int hashCode() {
            return myUsageType.hashCode();
        }

        @Override
        public String toString() {
            return "Type:" + myUsageType.toString(new UsageViewPresentation());
        }
    }
}
