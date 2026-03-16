/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.usage.rule;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.usage.UsageView;
import consulo.usage.UsageViewSettings;


/**
 * @author max
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface UsageGroupingRuleProvider {
    ExtensionPointName<UsageGroupingRuleProvider> EP_NAME = ExtensionPointName.create(UsageGroupingRuleProvider.class);

    
    UsageGroupingRule[] getActiveRules(Project project);

    
    default UsageGroupingRule[] getActiveRules(Project project, UsageViewSettings usageViewSettings) {
        return getActiveRules(project);
    }

    
    AnAction[] createGroupingActions(UsageView view);
}
