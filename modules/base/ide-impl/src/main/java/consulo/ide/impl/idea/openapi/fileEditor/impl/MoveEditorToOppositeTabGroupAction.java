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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.impl.internal.FileEditorHistoryUtil;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.HistoryEntry;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;

/**
 * User: anna
 * Date: Apr 18, 2005
 */
public class MoveEditorToOppositeTabGroupAction extends AnAction implements DumbAware {

  @RequiredUIAccess
  @Override
  public void actionPerformed(final AnActionEvent event) {
    final VirtualFile vFile = event.getData(VirtualFile.KEY);
    final Project project = event.getData(Project.KEY);
    if (vFile == null || project == null) {
      return;
    }
    final FileEditorWindow window = event.getData(FileEditorWindow.DATA_KEY);
    if (window != null) {
      final FileEditorWindow[] siblings = window.findSiblings();
      if (siblings.length == 1) {
        final FileEditorWithProviderComposite editorComposite = window.getSelectedEditor();
        final HistoryEntry entry = FileEditorHistoryUtil.currentStateAsHistoryEntry(editorComposite);
        ((FileEditorManagerImpl)FileEditorManagerEx.getInstanceEx(project))
          .openFileImpl3(UIAccess.current(), siblings[0], vFile, true, entry, true);
        window.closeFile(vFile);
      }
    }
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final VirtualFile vFile = e.getData(VirtualFile.KEY);
    final FileEditorWindow window = e.getData(FileEditorWindow.DATA_KEY);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      presentation.setVisible(isEnabled(vFile, window));
    }
    else {
      presentation.setEnabled(isEnabled(vFile, window));
    }
  }

  private static boolean isEnabled(VirtualFile vFile, FileEditorWindow window) {
    if (vFile != null && window != null) {
      final FileEditorWindow[] siblings = window.findSiblings();
      if (siblings.length == 1) {
        return true;
      }
    }
    return false;
  }
}
