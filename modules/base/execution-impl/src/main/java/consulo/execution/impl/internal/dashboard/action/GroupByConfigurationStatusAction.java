// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.impl.internal.dashboard.tree.StatusDashboardGroupingRule;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "RunDashboard.GroupByStatus")
public final class GroupByConfigurationStatusAction extends RunDashboardGroupingRuleToggleAction {

  @Override
  protected @Nonnull String getRuleName() {
    return StatusDashboardGroupingRule.NAME;
  }

  @Override
  protected boolean isEnabledByDefault() {
    return false;
  }
}
