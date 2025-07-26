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
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.OpenYoutubeAction;
import consulo.ide.impl.wm.impl.welcomeScreen.JoinDiscordChannelAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-07-26
 */
@ActionImpl(
    id = "HelpMenu",
    children = {
        @ActionRef(type = GotoActionAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = HelpTopicsAction.class),
        @ActionRef(type = ShowTipsAction.class),
        @ActionRef(id = "ProductivityGude"),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ShowLogAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = OnlineDocAction.class),
        @ActionRef(type = RefCardAction.class),
        @ActionRef(type = OpenYoutubeAction.class),
        @ActionRef(type = JoinDiscordChannelAction.class),
        @ActionRef(id = "TechnicalSupport"),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(id = "CheckForUpdate"),
        @ActionRef(type = AboutAction.class),
    }
)
public class HelpMenuGroup extends DefaultActionGroup implements DumbAware {
    public HelpMenuGroup() {
        super(ActionLocalize.groupHelpmenuText(), true);
    }
}
