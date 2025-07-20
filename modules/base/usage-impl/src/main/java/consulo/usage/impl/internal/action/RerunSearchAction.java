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
package consulo.usage.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.ReadAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.image.Image;
import consulo.usage.UsageView;
import consulo.usage.internal.UsageViewEx;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author gregsh
 */
@ActionImpl(id = "UsageView.Rerun", shortcutFrom = @ActionRef(id = IdeActions.ACTION_RERUN))
public class RerunSearchAction extends DumbAwareAction {
    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsRerun();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (e.getData(UsageView.USAGE_VIEW_KEY) instanceof UsageViewEx usageView) {
            usageView.refreshUsages();
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean enabled = e.getData(UsageView.USAGE_VIEW_KEY) instanceof UsageViewEx usageView
            && Objects.equals(ReadAction.compute(usageView::canPerformReRun), Boolean.TRUE);
        e.getPresentation().setEnabled(enabled);
    }
}
