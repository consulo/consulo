/*
 * Copyright 2013 must-be.org
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
package org.consulo.vfs.backgroundTask;

import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import org.consulo.util.pointers.Named;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 1:15/07.10.13
 */
public abstract class BackgroundTaskByVfsChangeProvider implements Named {
  public static final FileTypeExtension<BackgroundTaskByVfsChangeProvider> EP =
          new FileTypeExtension<BackgroundTaskByVfsChangeProvider>("com.intellij.taskByVfsChange");

  public boolean validate(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    return true;
  }

  public abstract void setDefaultParameters(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull BackgroundTaskByVfsParameters parameters);

  @NotNull
  public String[] getGeneratedFiles(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    PsiManager psiManager = PsiManager.getInstance(project);
    PsiFile file = psiManager.findFile(virtualFile);
    if(file != null) {
      return getGeneratedFiles(file);
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @NotNull
  public String[] getGeneratedFiles(@NotNull PsiFile psiFile) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }
}
