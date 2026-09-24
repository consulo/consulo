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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.ui.ex.content.ContentUI;

/**
 * The panel the run dashboard shows the content of the selected configuration in - a swing panel needs an awt
 * hierarchy to be drawn, a unified frontend gets a panel of its own.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RunDashboardContentUIFactory {
    static RunDashboardContentUIFactory getInstance() {
        return Application.get().getInstance(RunDashboardContentUIFactory.class);
    }

    ContentUI createContentUI();
}
