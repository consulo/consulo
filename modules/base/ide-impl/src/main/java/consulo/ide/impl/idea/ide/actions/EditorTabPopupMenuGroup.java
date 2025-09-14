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
import consulo.ide.impl.idea.openapi.fileEditor.impl.MoveEditorToOppositeTabGroupAction;
import consulo.ide.impl.idea.openapi.fileEditor.impl.ReopenClosedTabAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-09-14
 */
@ActionImpl(
    id = "EditorTabPopupMenu",
    children = {
        @ActionRef(type = CloseEditorsGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CopyPathsAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = SplitVerticallyAction.class),
        @ActionRef(type = SplitHorizontallyAction.class),
        @ActionRef(type = MoveEditorToOppositeTabGroupAction.class),
        @ActionRef(type = ChangeSplitterOrientationAction.class),
        @ActionRef(type = PinActiveTabAction.class),
        @ActionRef(type = TabsPlacementGroup.class),
        @ActionRef(type = TabsAlphabeticalModeSwitcher.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = NextTabAction.class),
        @ActionRef(type = PreviousTabAction.class),
        @ActionRef(type = ReopenClosedTabAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = UnsplitAction.class),
        @ActionRef(type = UnsplitAllAction.class),
        @ActionRef(type = EditorTabPopupMenuExGroup.class)
    }
)
public class EditorTabPopupMenuGroup extends DefaultActionGroup implements DumbAware {
    public EditorTabPopupMenuGroup() {
        super(ActionLocalize.groupNewelementinmenuText(), false);
    }
}
