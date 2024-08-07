/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SelectAllAction extends OccurrenceAction {
  public SelectAllAction() {
    super(IdeActions.ACTION_SELECT_ALL_OCCURRENCES, AllIcons.Actions.CheckMulticaret);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    search.selectAllOccurrences();
    search.close();
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    List<Shortcut> shortcuts = new ArrayList<>();
    AnAction selectAllOccurrences = ActionManager.getInstance().getAction(IdeActions.ACTION_SELECT_ALL_OCCURRENCES);
    if (selectAllOccurrences != null) {
      ContainerUtil.addAll(shortcuts, selectAllOccurrences.getShortcutSet().getShortcuts());
    }
    ContainerUtil.addAll(shortcuts, CommonShortcuts.ALT_ENTER.getShortcuts());
    return Utils.shortcutSetOf(shortcuts);
  }
}
