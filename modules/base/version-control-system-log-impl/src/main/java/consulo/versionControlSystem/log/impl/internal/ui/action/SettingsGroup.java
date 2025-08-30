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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

/**
 * @author UNV
 * @since 2025-08-28
 */
@ActionImpl(
    id = "Vcs.Log.Settings",
    children = {
        @ActionRef(type = ShowRootsColumnAction.class),
        @ActionRef(type = CompactReferencesViewAction.class),
        @ActionRef(type = ShowTagNamesAction.class),
        @ActionRef(type = ShowLongEdgesAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CollapseGraphAction.class),
        @ActionRef(type = ExpandGraphAction.class),
        @ActionRef(type = HighlightersActionGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(id = IdeActions.ACTION_CONTEXT_HELP)
    }
)
public class SettingsGroup extends DefaultActionGroup implements DumbAware {
    public SettingsGroup() {
        super(VersionControlSystemLogLocalize.groupSettingsText(), VersionControlSystemLogLocalize.groupSettingsDescription(), false);
    }
}
