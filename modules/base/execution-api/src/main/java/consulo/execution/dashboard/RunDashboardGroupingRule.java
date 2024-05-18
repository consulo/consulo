/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.dashboard;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.ui.view.tree.AbstractTreeNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Action for grouping items in a run dashboard (services) tree.
 * Grouping rules are applied to dashboard nodes according to their order defined in plug-in configuration.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RunDashboardGroupingRule {
  /**
   * @param node node which should be grouped by this grouping rule.
   * @return a group which node belongs to or {@code null} if node could not be grouped by this rule.
   */
  @Nullable
  RunDashboardGroup getGroup(AbstractTreeNode<?> node);

  /**
   * Returns a unique identifier for the rule.
   *
   * @return the rule identifier.
   */
  @Nonnull
  String getName();
}
