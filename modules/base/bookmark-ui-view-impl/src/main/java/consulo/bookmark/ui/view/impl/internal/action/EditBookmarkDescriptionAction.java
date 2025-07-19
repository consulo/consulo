/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.bookmark.localize.BookmarkLocalize;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

import javax.swing.*;

@ActionImpl(id = "EditBookmark")
public class EditBookmarkDescriptionAction extends DumbAwareAction {
    public EditBookmarkDescriptionAction() {
        super(
            BookmarkLocalize.actionBookmarkEditDescription(),
            BookmarkLocalize.actionBookmarkEditDescriptionDescription(),
            PlatformIconGroup.actionsEdit()
        );

        setShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(Platform.current().os().isMac() ? "meta ENTER" : "control ENTER")));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);

        BookmarkInContextInfo info = new BookmarkInContextInfo(e.getDataContext(), project).invoke();
        if (info.getFile() == null) {
            return;
        }

        Bookmark bookmark = info.getBookmarkAtPlace();
        if (bookmark == null) {
            return;
        }

        BookmarkManager.getInstance(project).editDescription(bookmark);
    }
}
