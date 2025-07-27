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
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.wm.impl.MaximizeToolWindowAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-07-26
 */
@ActionImpl(
    id = "ActiveToolwindowGroup",
    children = {
        @ActionRef(type = HideToolWindowAction.class),
        @ActionRef(type = HideSideWindowsAction.class),
        @ActionRef(type = HideAllToolWindowsAction.class),
        @ActionRef(type = CloseActiveTabAction.class),
        @ActionRef(type = JumpToLastWindowAction.class),
        @ActionRef(type = MaximizeToolWindowAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = TogglePinnedModeAction.class),
        @ActionRef(type = ToggleDockModeAction.class),
        @ActionRef(type = ToggleFloatingModeAction.class),
        @ActionRef(type = ToggleWindowedModeAction.class),
        @ActionRef(type = ToggleSideModeAction.class),
        @ActionRef(type = ToggleContentUiTypeAction.class),
        @ActionRef(type = ShowContentAction.class),
        @ActionRef(type = ResizeToolWindowGroup.class)
    }
)
public class ActiveToolWindowGroup extends DefaultActionGroup implements DumbAware {
    public ActiveToolWindowGroup() {
        super(ActionLocalize.groupActivetoolwindowgroupText(), true);
    }
}
