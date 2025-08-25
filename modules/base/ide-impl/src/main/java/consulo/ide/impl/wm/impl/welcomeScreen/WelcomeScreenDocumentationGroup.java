/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.wm.impl.welcomeScreen;

import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.ide.actions.HelpTopicsAction;
import consulo.ide.impl.idea.ide.actions.RefCardAction;
import consulo.ide.impl.idea.ide.actions.ShowTipsAction;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.DevelopPluginsAction;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.OpenYoutubeAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author VISTALL
 * @since 2022-06-26
 */
@ActionImpl(
    id = IdeActions.GROUP_WELCOME_SCREEN_DOC,
    children = {
        @ActionRef(type = HelpTopicsAction.class),
        @ActionRef(type = ShowTipsAction.class),
        @ActionRef(type = RefCardAction.class),
        @ActionRef(type = OpenYoutubeAction.class),
        @ActionRef(type = JoinDiscordChannelAction.class),
        @ActionRef(type = DevelopPluginsAction.class)
    }
)
public class WelcomeScreenDocumentationGroup extends DefaultActionGroup {
}
