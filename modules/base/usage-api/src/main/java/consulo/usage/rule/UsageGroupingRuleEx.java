/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageTarget;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @deprecated override {@link UsageGroupingRule#getParentGroupsFor(Usage, UsageTarget[])} instead.
 */
public interface UsageGroupingRuleEx extends UsageGroupingRule {
    @Nullable
    UsageGroup groupUsage(@Nonnull Usage usage, @Nonnull UsageTarget[] targets);

    @Nonnull
    @Override
    default List<UsageGroup> getParentGroupsFor(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
        return ContainerUtil.createMaybeSingletonList(groupUsage(usage, targets));
    }
}
