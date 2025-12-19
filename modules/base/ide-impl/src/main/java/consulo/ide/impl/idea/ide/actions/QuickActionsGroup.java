/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.documentation.actions.ShowQuickDocInfoAction;
import consulo.ide.impl.idea.codeInsight.hint.actions.ShowImplementationsAction;
import consulo.localize.LocalizeValue;
import consulo.project.ui.impl.internal.wm.action.ToolWindowsGroup;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-09-21
 */
@ActionImpl(
    id = "QuickActions",
    children = {
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ShowImplementationsAction.class),
        @ActionRef(type = ShowQuickDocInfoAction.class)
    },
    parents = @ActionParentRef(
        value = @ActionRef(type = ViewMenuGroup.class),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(type = ToolWindowsGroup.class)
    )
)
public class QuickActionsGroup extends DefaultActionGroup implements DumbAware {
    public QuickActionsGroup() {
        super(LocalizeValue.absent(), false);
    }
}
