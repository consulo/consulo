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
package consulo.ide.impl.idea.ide.actionMacro.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-05
 */
@ActionImpl(
    id = "StandardMacroActions",
    children = {
        @ActionRef(type = PlaybackLastMacroAction.class),
        @ActionRef(type = StartStopMacroRecordingAction.class),
        @ActionRef(type = EditMacrosAction.class),
        @ActionRef(type = PlaySavedMacros.class)
    }
)
public class StandardMacroActionsGroup extends DefaultActionGroup implements DumbAware {
    public StandardMacroActionsGroup() {
        super(ActionLocalize.groupStandardmacroactionsText(), false);
    }
}
