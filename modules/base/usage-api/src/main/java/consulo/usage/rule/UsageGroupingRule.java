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
package consulo.usage.rule;

import consulo.application.dumb.DumbAware;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageTarget;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * A rule specifying how specific Usage View elements should be grouped.
 *
 * During indexing, only instances that implement {@link DumbAware} are executed.
 */
public interface UsageGroupingRule {
    UsageGroupingRule[] EMPTY_ARRAY = new UsageGroupingRule[0];

    /**
     * Return list of nested parent groups for a usage. The specified usage will be placed into the last group from the list, that group
     * will be placed under the next to last group, etc.
     * <p>If the rule returns at most one parent group extend {@link SingleParentUsageGroupingRule} and override
     * {@link SingleParentUsageGroupingRule#getParentGroupFor getParentGroupFor} instead.</p>
     */
    @Nonnull
    default List<UsageGroup> getParentGroupsFor(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
        return ContainerUtil.createMaybeSingletonList(groupUsage(usage));
    }

    /**
     * Override this method to change order in which rules are applied. Rules with smaller ranks are applied earlier, i.e. parent groups
     * returned by them will be placed closer to the tree root.
     */
    default int getRank() {
        return Integer.MAX_VALUE;
    }

    /**
     * @deprecated extend {@link SingleParentUsageGroupingRule} and override {@link SingleParentUsageGroupingRule#getParentGroupFor getParentGroupFor} instead
     */
    @Nullable
    default UsageGroup groupUsage(@Nonnull Usage usage) {
        throw new UnsupportedOperationException();
    }
}
