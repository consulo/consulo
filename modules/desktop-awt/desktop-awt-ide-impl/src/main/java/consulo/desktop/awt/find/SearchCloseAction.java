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
package consulo.desktop.awt.find;

import consulo.find.localize.FindLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 2024-12-30
 */
public class SearchCloseAction extends DumbAwareAction {
    @Nonnull
    private final Runnable myCloseAction;

    public SearchCloseAction(@Nonnull Runnable closeAction) {
        super(FindLocalize.tooltipCloseSearchBarEscape(), LocalizeValue.of(), PlatformIconGroup.actionsCancel());
        myCloseAction = closeAction;

        setShortcutSet(new CustomShortcutSet(KeyEvent.VK_ESCAPE));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        myCloseAction.run();
    }
}
