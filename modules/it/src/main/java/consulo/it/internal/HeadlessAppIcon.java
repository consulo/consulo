/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.ui.TaskBar;
import consulo.ui.Window;
import consulo.ui.ex.AppIcon;
import jakarta.inject.Singleton;

/**
 * The production taskbar icon implementations live in the desktop modules; dumb mode progress reports its fraction
 * through {@link AppIcon}, so the headless application swallows those calls.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessAppIcon implements AppIcon {
    @Override
    public boolean setProgress(ComponentManager project, Object processId, TaskBar.ProgressScheme progressScheme, double value, boolean isOk) {
        return false;
    }

    @Override
    public boolean hideProgress(ComponentManager project, Object processId) {
        return false;
    }

    @Override
    public void setErrorBadge(ComponentManager project, String text) {
    }

    @Override
    public void setOkBadge(ComponentManager project, boolean visible) {
    }

    @Override
    public void requestAttention(ComponentManager project, boolean critical) {
    }

    @Override
    public void requestFocus(Window frame) {
    }
}
