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
package consulo.fileEditor.util;

import consulo.document.util.FileContentUtilCore;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * @author peter
 */
public class FileContentUtil extends FileContentUtilCore {
  public static void reparseFiles(@Nonnull final Project project, @Nonnull final Collection<? extends VirtualFile> files, final boolean includeOpenFiles) {
    LinkedHashSet<VirtualFile> fileSet = new LinkedHashSet<VirtualFile>(files);
    if (includeOpenFiles) {
      for (VirtualFile open : FileEditorManager.getInstance(project).getOpenFiles()) {
        if (!fileSet.contains(open)) {
          fileSet.add(open);
        }
      }
    }
    FileContentUtilCore.reparseFiles(fileSet);
  }

  public static void reparseOpenedFiles() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      reparseFiles(project, Collections.<VirtualFile>emptyList(), true);
    }
  }
}
