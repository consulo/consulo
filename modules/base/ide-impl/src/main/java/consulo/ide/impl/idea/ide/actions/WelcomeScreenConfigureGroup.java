/*
 * Copyright 2013-2025 consulo.io
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
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-10-02
 */
@ActionImpl(
    id = "WelcomeScreen.Configure",
    children = {
        @ActionRef(type = ShowSettingsAction.class),
        @ActionRef(type = ShowPluginManagerAction.class),
        @ActionRef(type = ImportSettingsAction.class),
        @ActionRef(type = ExportSettingsAction.class),
        @ActionRef(type = CreateDesktopEntryAction.class),
        @ActionRef(id = IdeActions.ACTION_CHECK_FOR_UPDATE),
        @ActionRef(type = ShowLogAction.class),
        @ActionRef(type = AboutAction.class)
    }
)
public class WelcomeScreenConfigureGroup extends DefaultActionGroup implements DumbAware {
    public WelcomeScreenConfigureGroup() {
        super(LocalizeValue.empty(), false);
    }
}
