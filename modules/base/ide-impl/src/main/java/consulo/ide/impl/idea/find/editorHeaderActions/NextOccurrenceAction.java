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
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.fileEditor.impl.internal.search.SearchSession;
import consulo.fileEditor.impl.internal.search.SearchUtils;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Shortcut;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;

public class NextOccurrenceAction extends PrevNextOccurrenceAction {
    public NextOccurrenceAction() {
        this(true);
    }

    public NextOccurrenceAction(boolean search) {
        super(IdeActions.ACTION_NEXT_OCCURENCE, search);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        SearchSession session = e.getRequiredData(SearchSession.KEY);
        if (session.hasMatches()) {
            session.searchForward();
        }
    }

    @Nonnull
    @Override
    protected List<Shortcut> getDefaultShortcuts() {
        return SearchUtils.shortcutsOf(IdeActions.ACTION_FIND_NEXT);
    }

    @Nonnull
    @Override
    protected List<Shortcut> getSingleLineShortcuts() {
        List<Shortcut> shortcuts = SearchUtils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN);
        if (mySearch) {
            return ContainerUtil.concat(shortcuts, Arrays.asList(CommonShortcuts.ENTER.getShortcuts()));
        }
        else {
            return shortcuts;
        }
    }
}
