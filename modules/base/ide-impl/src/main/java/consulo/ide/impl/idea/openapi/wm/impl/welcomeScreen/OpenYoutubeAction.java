/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.webBrowser.BrowserUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

@ActionImpl(id = "Help.Youtube")
public class OpenYoutubeAction extends DumbAwareAction {
    public OpenYoutubeAction() {
        super(ActionLocalize.actionHelpYoutubeText(), ActionLocalize.actionHelpYoutubeDescription(), PlatformIconGroup.generalYoutube());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        BrowserUtil.browse("http://www.youtube.com/user/ConsuloIDE");
    }
}