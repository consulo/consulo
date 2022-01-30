/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.usages.rules;

import consulo.ui.ex.action.AnAction;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewSettings;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public interface UsageGroupingRuleProvider {
  ExtensionPointName<UsageGroupingRuleProvider> EP_NAME = ExtensionPointName.create("consulo.usageGroupingRuleProvider");

  @Nonnull
  UsageGroupingRule[] getActiveRules(@Nonnull Project project);

  @Nonnull
  default UsageGroupingRule[] getActiveRules(@Nonnull Project project, @Nonnull UsageViewSettings usageViewSettings) {
    return getActiveRules(project);
  }

  @Nonnull
  AnAction[] createGroupingActions(@Nonnull UsageView view);
}
