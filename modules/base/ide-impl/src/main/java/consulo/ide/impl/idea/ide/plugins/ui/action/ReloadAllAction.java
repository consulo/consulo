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
package consulo.ide.impl.idea.ide.plugins.ui.action;

import consulo.ide.impl.idea.ide.plugins.ui.PluginTab;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class ReloadAllAction extends DumbAwareAction {
    private final PluginTab[] myTabs;

    public ReloadAllAction(PluginTab... tabs) {
        super("Reload Plugins", null, PlatformIconGroup.actionsRefresh());
        myTabs = tabs;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        for (PluginTab tab : myTabs) {
            tab.reload();
        }
    }
}
