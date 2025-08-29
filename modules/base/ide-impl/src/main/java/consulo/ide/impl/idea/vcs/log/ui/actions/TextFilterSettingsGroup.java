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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogActionPlaces;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

/**
 * @author UNV
 * @since 2025-08-28
 */
@ActionImpl(
    id = VcsLogActionPlaces.TEXT_FILTER_SETTINGS_ACTION_GROUP,
    children = {
        @ActionRef(type = EnableFilterByRegexAction.class),
        @ActionRef(type = EnableMatchCaseAction.class)
    }
)
public class TextFilterSettingsGroup extends DefaultActionGroup implements DumbAware {
    public TextFilterSettingsGroup() {
        super(
            VersionControlSystemLogLocalize.groupTextFilterSettingsText(),
            VersionControlSystemLogLocalize.groupTextFilterSettingsDescription(),
            false
        );
    }
}
