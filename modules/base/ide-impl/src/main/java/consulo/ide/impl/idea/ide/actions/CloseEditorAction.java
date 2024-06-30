
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;

public class CloseEditorAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(Project.KEY);

    FileEditorManager editorManager = getEditorManager(project);
    FileEditorWindow window = e.getData(FileEditorWindow.DATA_KEY);
    VirtualFile file = null;
    if (window == null) {
      window = editorManager.getActiveWindow().getResult();
      if (window != null) {
        file = window.getSelectedFile();
      }
    }
    else {
      file = e.getData(VirtualFile.KEY);
    }
    if (file != null) {
      editorManager.closeFile(file, window);
    }
  }

  private static FileEditorManager getEditorManager(Project project) {
    return FileEditorManager.getInstance(project);
  }

  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent event){
    final Presentation presentation = event.getPresentation();
    final Project project = event.getData(Project.KEY);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    if (ActionPlaces.EDITOR_POPUP.equals(event.getPlace()) || ActionPlaces.EDITOR_TAB_POPUP.equals(event.getPlace())) {
      presentation.setTextValue(IdeLocalize.actionClose());
    }
    FileEditorWindow window = event.getData(FileEditorWindow.DATA_KEY);
    if (window == null) {
      window = getEditorManager(project).getActiveWindow().getResult();
    }
    presentation.setEnabled(window != null && window.getTabCount() > 0);
  }
}
