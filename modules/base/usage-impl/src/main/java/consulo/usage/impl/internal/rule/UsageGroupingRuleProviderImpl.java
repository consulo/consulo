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
package consulo.usage.impl.internal.rule;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.usage.RuleAction;
import consulo.usage.UsageView;
import consulo.usage.UsageViewSettings;
import consulo.usage.impl.internal.action.GroupByFileStructureAction;
import consulo.usage.internal.UsageViewEx;
import consulo.usage.localize.UsageLocalize;
import consulo.usage.rule.FileGroupingRule;
import consulo.usage.rule.FileStructureGroupRuleProvider;
import consulo.usage.rule.UsageGroupingRule;
import consulo.usage.rule.UsageGroupingRuleProvider;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
@ExtensionImpl
public class UsageGroupingRuleProviderImpl implements UsageGroupingRuleProvider {
    @Nonnull
    @Override
    public UsageGroupingRule[] getActiveRules(@Nonnull Project project) {
        List<UsageGroupingRule> rules = new ArrayList<>();
        rules.add(new NonCodeUsageGroupingRule(project));
        if (UsageViewSettings.getInstance().GROUP_BY_SCOPE) {
            rules.add(new UsageScopeGroupingRule());
        }
        if (UsageViewSettings.getInstance().GROUP_BY_USAGE_TYPE) {
            rules.add(new UsageTypeGroupingRule());
        }
        if (UsageViewSettings.getInstance().GROUP_BY_MODULE) {
            rules.add(new ModuleGroupingRule());
        }
        if (UsageViewSettings.getInstance().GROUP_BY_PACKAGE) {
            rules.add(new DirectoryGroupingRule(project));
        }
        if (UsageViewSettings.getInstance().GROUP_BY_FILE_STRUCTURE) {
            for (FileStructureGroupRuleProvider ruleProvider : FileStructureGroupRuleProvider.EP_NAME.getExtensionList()) {
                UsageGroupingRule rule = ruleProvider.getUsageGroupingRule(project);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }
        else {
            rules.add(new FileGroupingRule(project));
        }

        return rules.toArray(new UsageGroupingRule[rules.size()]);
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AnAction[] createGroupingActions(UsageView view) {
        UsageViewEx impl = (UsageViewEx)view;
        JComponent component = impl.getComponent();

        GroupByModuleTypeAction groupByModuleTypeAction = new GroupByModuleTypeAction(impl);
        groupByModuleTypeAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK)),
            component,
            impl
        );

        GroupByFileStructureAction groupByFileStructureAction = createGroupByFileStructureAction(impl);

        GroupByScopeAction groupByScopeAction = new GroupByScopeAction(impl);

        GroupByPackageAction groupByPackageAction = new GroupByPackageAction(impl);
        groupByPackageAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)),
            component,
            impl
        );

        if (view.getPresentation().isCodeUsages()) {
            GroupByUsageTypeAction groupByUsageTypeAction = new GroupByUsageTypeAction(impl);
            groupByUsageTypeAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK)),
                component,
                impl
            );
            return new AnAction[]{
                groupByUsageTypeAction,
                groupByScopeAction,
                groupByModuleTypeAction,
                groupByPackageAction,
                groupByFileStructureAction,
            };
        }
        else {
            return new AnAction[]{
                groupByScopeAction,
                groupByModuleTypeAction,
                groupByPackageAction,
                groupByFileStructureAction,
            };
        }
    }

    @RequiredUIAccess
    public static GroupByFileStructureAction createGroupByFileStructureAction(UsageViewEx impl) {
        JComponent component = impl.getComponent();
        GroupByFileStructureAction groupByFileStructureAction = new GroupByFileStructureAction();
        groupByFileStructureAction.registerCustomShortcutSet(new CustomShortcutSet(
            KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK)),
            component,
            impl
        );

        return groupByFileStructureAction;
    }

    private static class GroupByUsageTypeAction extends RuleAction {
        private GroupByUsageTypeAction(UsageViewEx view) {
            super(view, UsageLocalize.actionGroupByUsageType(), PlatformIconGroup.generalFilter()); //TODO: special icon
        }

        @Override
        protected boolean getOptionValue() {
            return UsageViewSettings.getInstance().GROUP_BY_USAGE_TYPE;
        }

        @Override
        protected void setOptionValue(boolean value) {
            UsageViewSettings.getInstance().GROUP_BY_USAGE_TYPE = value;
        }
    }

    private static class GroupByScopeAction extends RuleAction {
        private GroupByScopeAction(UsageViewEx view) {
            super(view, "Group by test/production", PlatformIconGroup.actionsGroupbytestproduction());
        }

        @Override
        protected boolean getOptionValue() {
            return UsageViewSettings.getInstance().GROUP_BY_SCOPE;
        }

        @Override
        protected void setOptionValue(boolean value) {
            UsageViewSettings.getInstance().GROUP_BY_SCOPE = value;
        }
    }

    private static class GroupByModuleTypeAction extends RuleAction {
        private GroupByModuleTypeAction(UsageViewEx view) {
            super(view, UsageLocalize.actionGroupByModule(), PlatformIconGroup.actionsGroupbymodule());
        }

        @Override
        protected boolean getOptionValue() {
            return UsageViewSettings.getInstance().GROUP_BY_MODULE;
        }

        @Override
        protected void setOptionValue(boolean value) {
            UsageViewSettings.getInstance().GROUP_BY_MODULE = value;
        }
    }

    private static class GroupByPackageAction extends RuleAction {
        private GroupByPackageAction(UsageViewEx view) {
            super(view, UsageLocalize.actionGroupByPackage(), PlatformIconGroup.actionsGroupbypackage());
        }

        @Override
        protected boolean getOptionValue() {
            return UsageViewSettings.getInstance().GROUP_BY_PACKAGE;
        }

        @Override
        protected void setOptionValue(boolean value) {
            UsageViewSettings.getInstance().GROUP_BY_PACKAGE = value;
        }
    }

}
