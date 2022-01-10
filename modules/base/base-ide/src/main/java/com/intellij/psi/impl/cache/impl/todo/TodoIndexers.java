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

package com.intellij.psi.impl.cache.impl.todo;

import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class TodoIndexers extends FileTypeExtension<DataIndexer<TodoIndexEntry, Integer, FileContent>> {
  public static TodoIndexers INSTANCE = new TodoIndexers();

  private TodoIndexers() {
    super("com.intellij.todoIndexer");
  }

  public static boolean belongsToProject(@Nonnull Project project, @Nonnull VirtualFile file) {
    //for (ExtraPlaceChecker checker : EP_NAME.getExtensionList()) {
    //  if (checker.accept(project, file)) {
    //    return true;
    //  }
    //}
    if (!ProjectFileIndex.getInstance(project).isInContent(file)) {
      return false;
    }
    return true;
  }
}
