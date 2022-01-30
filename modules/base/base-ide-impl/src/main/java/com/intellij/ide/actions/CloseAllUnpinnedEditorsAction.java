/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import com.intellij.openapi.util.Pair;
import consulo.fileEditor.impl.EditorComposite;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWithProviderComposite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public class CloseAllUnpinnedEditorsAction extends CloseEditorsActionBase {
  @Override
  protected boolean isFileToClose(EditorComposite editor, final EditorWindow window) {
    return !window.isFilePinned(editor.getFile());
  }

  @Override
  protected String getPresentationText(final boolean inSplitter) {
    if (inSplitter) {
      return IdeBundle.message("action.close.all.unpinned.editors.in.tab.group");
    }
    else {
      return IdeBundle.message("action.close.all.unpinned.editors");
    }
  }

  @Override
  protected boolean isActionEnabled(final Project project, final AnActionEvent event) {
    final List<Pair<EditorComposite, EditorWindow>> filesToClose = getFilesToClose(event);
    if (filesToClose.isEmpty()) return false;
    Set<EditorWindow> checked = new HashSet<>();
    for (Pair<EditorComposite, EditorWindow> pair : filesToClose) {
      final EditorWindow window = pair.second;
      if (!checked.contains(window)) {
        checked.add(window);
        if (hasPinned(window)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean hasPinned(final EditorWindow window) {
    for (EditorWithProviderComposite e : window.getEditors()) {
      if (e.isPinned()) {
        return true;
      }
    }
    return false;
  }
}