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
package consulo.ide.impl.idea.usages.impl;

import consulo.project.DumbService;
import consulo.project.Project;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageTarget;
import consulo.usage.rule.UsageFilteringRule;
import consulo.usage.rule.UsageGroupingRule;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author max
 */
class UsageNodeTreeBuilder {
  private final GroupNode myRoot;
  private final Project myProject;
  private final UsageTarget[] myTargets;
  private UsageGroupingRule[] myGroupingRules;
  private UsageFilteringRule[] myFilteringRules;

  UsageNodeTreeBuilder(@Nonnull UsageTarget[] targets, @Nonnull UsageGroupingRule[] groupingRules, @Nonnull UsageFilteringRule[] filteringRules, @Nonnull GroupNode root, @Nonnull Project project) {
    myTargets = targets;
    myGroupingRules = groupingRules;
    myFilteringRules = filteringRules;
    myRoot = root;
    myProject = project;
  }

  public void setGroupingRules(@Nonnull UsageGroupingRule[] rules) {
    myGroupingRules = rules;
  }

  void setFilteringRules(@Nonnull UsageFilteringRule[] rules) {
    myFilteringRules = rules;
  }

  public boolean isVisible(@Nonnull Usage usage) {
    return Arrays.stream(myFilteringRules).allMatch(rule -> rule.isVisible(usage, myTargets));
  }

  UsageNode appendOrGet(@Nonnull Usage usage, boolean filterDuplicateLines, @Nonnull Consumer<? super Node> edtInsertedUnderQueue) {
    if (!isVisible(usage)) return null;

    boolean dumb = DumbService.isDumb(myProject);

    GroupNode groupNode = myRoot;
    for (int i = 0; i < myGroupingRules.length; i++) {
      UsageGroupingRule rule = myGroupingRules[i];
      if (dumb && !DumbService.isDumbAware(rule)) continue;

      List<UsageGroup> groups = rule.getParentGroupsFor(usage, myTargets);
      for (UsageGroup group : groups) {
        groupNode = groupNode.addOrGetGroup(group, i, edtInsertedUnderQueue);
      }
    }

    return groupNode.addOrGetUsage(usage, filterDuplicateLines, edtInsertedUnderQueue);
  }
}
