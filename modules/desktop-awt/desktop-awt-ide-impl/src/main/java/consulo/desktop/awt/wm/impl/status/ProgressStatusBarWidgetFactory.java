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
package consulo.desktop.awt.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-11-19
 */
@ExtensionImpl(id = "progress", order = "first")
public class ProgressStatusBarWidgetFactory implements StatusBarWidgetFactory {
    @Nonnull
    @Override
    public String getDisplayName() {
        return "Progress";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project) {
        return true;
    }

    @Nonnull
    @Override
    public StatusBarWidget createWidget(@Nonnull Project project) {
        return new InfoAndProgressPanel(() -> project);
    }

    @Override
    public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }
}
