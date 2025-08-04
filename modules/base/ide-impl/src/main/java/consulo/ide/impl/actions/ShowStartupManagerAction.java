/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.actions;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ide.impl.startup.customize.StartupCustomizeManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2021-01-09
 */
@ActionImpl(id = "ShowStartupManagerAction")
public class ShowStartupManagerAction extends DumbAwareAction {
    private final Provider<StartupCustomizeManager> myStartupCustomizeManager;

    @Inject
    public ShowStartupManagerAction(Provider<StartupCustomizeManager> startupCustomizeManager) {
        super(ActionLocalize.actionShowstartupmanageractionText(), ActionLocalize.actionShowstartupmanageractionDescription());
        myStartupCustomizeManager = startupCustomizeManager;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        myStartupCustomizeManager.get().showAsync(false);
    }
}
