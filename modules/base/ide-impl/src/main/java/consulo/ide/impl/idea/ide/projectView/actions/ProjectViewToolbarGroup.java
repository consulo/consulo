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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.actions.CollapseAllAction;
import consulo.ide.impl.idea.ide.actions.ExpandAllAction;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author VISTALL
 * @since 2025-05-25
 */
@ActionImpl(id = ActionPlaces.PROJECT_VIEW_TOOLBAR, children = {
    //@ActionRef(type = NewElementAction.class),
    @ActionRef(type = AnSeparator.class),
    @ActionRef(type = SelectFileAction.class),
    @ActionRef(type = ExpandAllAction.class),
    @ActionRef(type = CollapseAllAction.class)
})
public class ProjectViewToolbarGroup extends DefaultActionGroup implements DumbAware {
}
