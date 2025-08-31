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
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2005-04-18
 */
public class MoveEditorToOppositeTabGroupAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent event) {
    VirtualFile vFile = event.getRequiredData(VirtualFile.KEY);
    Project project = event.getRequiredData(Project.KEY);
    FileEditorWindow window = event.getRequiredData(FileEditorWindow.DATA_KEY);
    FileEditorWindow[] siblings = window.findSiblings();
    if (siblings.length == 1) {
      FileEditorWithProviderComposite editorComposite = window.getSelectedEditor();
      HistoryEntry entry = FileEditorHistoryUtil.currentStateAsHistoryEntry(editorComposite);
      ((FileEditorManagerImpl)FileEditorManagerEx.getInstanceEx(project))
        .openFileImpl3(UIAccess.current(), siblings[0], vFile, true, entry, true);
      window.closeFile(vFile);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    VirtualFile vFile = e.getData(VirtualFile.KEY);
    FileEditorWindow window = e.getData(FileEditorWindow.DATA_KEY);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      presentation.setVisible(isEnabled(vFile, window));
    }
    else {
      presentation.setEnabled(isEnabled(vFile, window));
    }
  }

  private static boolean isEnabled(VirtualFile vFile, FileEditorWindow window) {
    if (vFile != null && window != null) {
      FileEditorWindow[] siblings = window.findSiblings();
      if (siblings.length == 1) {
        return true;
      }
    }
    return false;
  }
}
