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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

/**
 * @author Vladimir Kondratyev
 */
@ActionImpl(id = "Help.KeymapReference")
public class RefCardAction extends AnAction implements DumbAware {
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        String helpId;

        if (Platform.current().os().isMac()) {
            helpId = "platform/keymap/mac/";
        }
        else {
            helpId = "platform/keymap/windows_linux/";
        }

        HelpManager.getInstance().invokeHelp(helpId);
    }
}
