// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.localize.VcsLocalize;

@ActionImpl(
    id = "ChangesView.GroupBy",
    children = {
        @ActionRef(type = SetDirectoryChangesGroupingAction.class),
        @ActionRef(type = SetModuleChangesGroupingAction.class),
        @ActionRef(type = SetRepositoryChangesGroupingAction.class)
    }
)
public class SelectChangesGroupingActionGroup extends DefaultActionGroup implements DumbAware, AnActionWithSyncUpdate {
    public SelectChangesGroupingActionGroup() {
        super(VcsLocalize.changesActionGroupBySeparator(), LocalizeValue.empty(), PlatformIconGroup.actionsGroupby());
        setPopup(true);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(SetChangesGroupingAction.getGroupingSupport(e) != null);
    }
}
