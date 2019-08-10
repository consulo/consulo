/*
 * Copyright 2013-2018 consulo.io
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
package consulo.fileEditor.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;
import consulo.logging.Logger;

import javax.annotation.Nullable;
import java.io.File;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public abstract class EditorsSplittersBase implements EditorsSplitters {
  private static final Logger LOG = Logger.getInstance(EditorsSplittersBase.class);

  protected final FileEditorManagerImpl myManager;

  protected EditorsSplittersBase(FileEditorManagerImpl manager) {
    myManager = manager;
  }

  @Override
  public final void updateFileName(@Nullable final VirtualFile updatedFile) {
    final EditorWindow[] windows = getWindows();
    for (int i = 0; i != windows.length; ++i) {
      for (VirtualFile file : windows[i].getFiles()) {
        if (updatedFile == null || file.getName().equals(updatedFile.getName())) {
          ((EditorWindowBase)windows[i]).updateFileName(file);
        }
      }
    }

    Project project = myManager.getProject();

    final IdeFrame frame = getFrame(project);
    if (frame != null) {
      VirtualFile file = getCurrentFile();

      File ioFile = file == null ? null : new File(file.getPresentableUrl());
      String fileTitle = null;
      if (file != null) {
        fileTitle = DumbService.isDumb(project) ? file.getName() : FrameTitleBuilder.getInstance().getFileTitle(project, file);
      }

      frame.setFileTitle(fileTitle, ioFile);
    }
  }

  protected IdeFrame getFrame(Project project) {
    final IdeFrame frame = WindowManagerEx.getInstance().getIdeFrame(project);
    LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode() || frame != null);
    return frame;
  }
}
