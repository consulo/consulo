/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.usage.rule;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.usage.UsageView;
import consulo.usage.UsageViewSettings;

import javax.annotation.Nonnull;

/**
 * @author max
 */
@Extension(ComponentScope.APPLICATION)
public interface UsageGroupingRuleProvider {
  ExtensionPointName<UsageGroupingRuleProvider> EP_NAME = ExtensionPointName.create(UsageGroupingRuleProvider.class);

  @Nonnull
  UsageGroupingRule[] getActiveRules(@Nonnull Project project);

  @Nonnull
  default UsageGroupingRule[] getActiveRules(@Nonnull Project project, @Nonnull UsageViewSettings usageViewSettings) {
    return getActiveRules(project);
  }

  @Nonnull
  AnAction[] createGroupingActions(@Nonnull UsageView view);
}
