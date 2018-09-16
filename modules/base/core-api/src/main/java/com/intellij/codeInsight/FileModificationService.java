/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

public abstract class FileModificationService {
  public static FileModificationService getInstance() {
    return ServiceManager.getService(FileModificationService.class);
  }

  public abstract boolean preparePsiElementsForWrite(@Nonnull Collection<? extends PsiElement> elements);
  public abstract boolean prepareFileForWrite(@Nullable final PsiFile psiFile);

  public boolean preparePsiElementForWrite(@Nullable PsiElement element) {
    PsiFile file = element == null ? null : element.getContainingFile();
    return prepareFileForWrite(file);
  }

  public boolean preparePsiElementsForWrite(@Nonnull PsiElement... elements) {
    return preparePsiElementsForWrite(Arrays.asList(elements));
  }

  public abstract boolean prepareVirtualFilesForWrite(@Nonnull Project project, @Nonnull Collection<VirtualFile> files);
}
