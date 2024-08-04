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
package consulo.ide.impl.wm.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
@ExtensionImpl(id = "InitToolWindows", order = "first")
public class InitToolWindowsActivity implements PostStartupActivity, DumbAware {
    public InitToolWindowsActivity() {
    }

    @RequiredUIAccess
    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        UIAccess.assetIsNotUIThread();

        ToolWindowManagerBase manager = (ToolWindowManagerBase) ToolWindowManager.getInstance(project);

        uiAccess.giveAsync(manager::initializeUI)
            .whenCompleteAsync((o, t) -> manager.initializeEditorComponent())
            .whenCompleteAsync((o, t) -> manager.registerToolWindowsFromBeans(uiAccess))
            .whenCompleteAsync((o, t) -> manager.postInitialize(), uiAccess)
            .whenCompleteAsync((o, t) -> manager.connectModuleExtensionListener(), uiAccess)
            .whenCompleteAsync((o, t) -> manager.activateOnProjectOpening(), uiAccess);
    }
}
