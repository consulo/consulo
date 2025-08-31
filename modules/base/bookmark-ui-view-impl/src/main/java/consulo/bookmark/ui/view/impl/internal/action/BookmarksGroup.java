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
package consulo.bookmark.ui.view.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-31
 */
@ActionImpl(
    id = "Bookmarks",
    children = {
        @ActionRef(type = ToggleBookmarkAction.class),
        @ActionRef(id = "ShowBookmarks"),
        @ActionRef(type = NextBookmarkAction.class),
        @ActionRef(type = PreviousBookmarkAction.class),

        @ActionRef(type = GotoBookmark0Action.class),
        @ActionRef(type = GotoBookmark1Action.class),
        @ActionRef(type = GotoBookmark2Action.class),
        @ActionRef(type = GotoBookmark3Action.class),
        @ActionRef(type = GotoBookmark4Action.class),
        @ActionRef(type = GotoBookmark5Action.class),
        @ActionRef(type = GotoBookmark6Action.class),
        @ActionRef(type = GotoBookmark7Action.class),
        @ActionRef(type = GotoBookmark8Action.class),
        @ActionRef(type = GotoBookmark9Action.class),

        @ActionRef(type = ToggleBookmark0Action.class),
        @ActionRef(type = ToggleBookmark1Action.class),
        @ActionRef(type = ToggleBookmark2Action.class),
        @ActionRef(type = ToggleBookmark3Action.class),
        @ActionRef(type = ToggleBookmark4Action.class),
        @ActionRef(type = ToggleBookmark5Action.class),
        @ActionRef(type = ToggleBookmark6Action.class),
        @ActionRef(type = ToggleBookmark7Action.class),
        @ActionRef(type = ToggleBookmark8Action.class),
        @ActionRef(type = ToggleBookmark9Action.class)
    }
)
public class BookmarksGroup extends DefaultActionGroup implements DumbAware {
    public BookmarksGroup() {
        super(ActionLocalize.groupBookmarksText(), false);
    }
}
