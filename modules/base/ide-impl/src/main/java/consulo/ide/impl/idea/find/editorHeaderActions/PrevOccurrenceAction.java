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
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class PrevOccurrenceAction extends PrevNextOccurrenceAction {
  public PrevOccurrenceAction() {
    this(true);
  }

  public PrevOccurrenceAction(boolean search) {
    super(IdeActions.ACTION_PREVIOUS_OCCURENCE, search);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    SearchSession session = e.getRequiredData(SearchSession.KEY);
    if (session.hasMatches()) session.searchBackward();
  }

  @Nonnull
  @Override
  protected List<Shortcut> getDefaultShortcuts() {
    return SearchUtils.shortcutsOf(IdeActions.ACTION_FIND_PREVIOUS);
  }

  @Nonnull
  @Override
  protected List<Shortcut> getSingleLineShortcuts() {
    if (mySearch) {
      return ContainerUtil.append(SearchUtils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_UP),
                                  new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null));
    } else {
      return SearchUtils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_UP);
    }
  }
}
