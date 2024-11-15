/*
 * Copyright 2013-2024 consulo.io
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
package consulo.application.ui.internal;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.ApplicationProperties;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-09-07
 */
@ActionImpl(id = "Internal", parents = @ActionParentRef(value = @ActionRef(id = IdeActions.TOOLS_MENU), anchor = ActionRefAnchor.LAST))
public class InternalActionGroup extends DefaultActionGroup implements DumbAware {
    public InternalActionGroup() {
        super(LocalizeValue.localizeTODO("Internal"), true);
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(ApplicationProperties.isInternal());
    }
}
