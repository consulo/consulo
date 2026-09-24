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
package consulo.execution.impl.internal.dashboard;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.content.ContentUI;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class AWTRunDashboardContentUIFactory implements RunDashboardContentUIFactory {
    @Override
    public ContentUI createContentUI() {
        return new PanelContentUI();
    }
}
