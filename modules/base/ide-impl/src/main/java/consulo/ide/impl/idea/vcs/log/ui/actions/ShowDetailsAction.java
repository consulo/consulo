/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogActionPlaces;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

@ActionImpl(id = VcsLogActionPlaces.VCS_LOG_SHOW_DETAILS_ACTION)
public class ShowDetailsAction extends BooleanPropertyToggleAction {
    public ShowDetailsAction() {
        super(
            VersionControlSystemLogLocalize.actionShowDetailsText(),
            VersionControlSystemLogLocalize.actionShowDetailsDescription(),
            PlatformIconGroup.actionsPreview()
        );
    }

    @Override
    protected VcsLogUiProperties.VcsLogUiProperty<Boolean> getProperty() {
        return MainVcsLogUiProperties.SHOW_DETAILS;
    }
}
