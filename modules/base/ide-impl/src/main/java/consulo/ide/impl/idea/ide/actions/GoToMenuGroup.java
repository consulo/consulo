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
import consulo.ide.impl.idea.codeInsight.daemon.impl.actions.GoToErrorGroup;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GoToMenuExGroup;
import consulo.ide.impl.idea.openapi.vcs.actions.GoToChangeMarkerGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-07-31
 */
@ActionImpl(
    id = "GoToMenu",
    children = {
        @ActionRef(type = GoToTargetExGroup.class),
        @ActionRef(type = GotoLineAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = BackAction.class),
        @ActionRef(type = ForwardAction.class),
        @ActionRef(type = JumpToLastEditAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = GoToCodeGroup.class),
        @ActionRef(type = GoToErrorGroup.class),
        @ActionRef(type = GoToMenuExGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = PreviousOccurenceAction.class),
        @ActionRef(type = NextOccurenceAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = GoToChangeMarkerGroup.class)
    }
)
public class GoToMenuGroup extends DefaultActionGroup implements DumbAware {
    public GoToMenuGroup() {
        super(ActionLocalize.groupGotomenuText(), true);
    }
}
