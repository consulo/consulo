/*
 * Copyright 2013-2021 consulo.io
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
package consulo.diff.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.diff.DiffEditorTabFilesManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
* @author VISTALL
* @since 23/09/2021
*/
@Singleton
@ServiceImpl
public class DefaultDiffTabFilesManager implements DiffEditorTabFilesManager {
  private final Project myProject;

  @Inject
  public DefaultDiffTabFilesManager(Project project) {
    myProject = project;
  }

  @Override
  public FileEditor[] showDiffFile(VirtualFile diffFile, boolean focusEditor) {
    return FileEditorManager.getInstance(myProject).openFile(diffFile, true);
  }
}
