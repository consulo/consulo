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
package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author peter
 */
public class FileReferenceHelperRegistrar {

  private FileReferenceHelperRegistrar() {
  }

  public static List<FileReferenceHelper> getHelpers() {
    return FileReferenceHelper.EP_NAME.getExtensionList();
  }

  /**
   * @deprecated this method is broken, please avoid using it, use getHelpers() instead
   */
  @Deprecated
  @Nonnull
  public static <T extends PsiFileSystemItem> FileReferenceHelper getNotNullHelper(@Nonnull T psiFileSystemItem) {
    FileReferenceHelper helper = getHelper(psiFileSystemItem);
    if (helper != null) {
      return helper;
    }
    List<FileReferenceHelper> helpers = getHelpers();
    return ContainerUtil.getLastItem(helpers);
  }

  /**
   * @deprecated this method is broken, please avoid using it, use getHelpers() instead
   */
  @Deprecated
  public static <T extends PsiFileSystemItem> FileReferenceHelper getHelper(@Nonnull final T psiFileSystemItem) {
    final VirtualFile file = psiFileSystemItem.getVirtualFile();
    if (file == null) return null;
    final Project project = psiFileSystemItem.getProject();
    return ContainerUtil.find(getHelpers(), fileReferenceHelper -> fileReferenceHelper.isMine(project, file));
  }

  public static <T extends PsiFileSystemItem> List<FileReferenceHelper> getHelpers(@Nonnull final T psiFileSystemItem) {
    final VirtualFile file = psiFileSystemItem.getVirtualFile();
    if (file == null) return null;
    final Project project = psiFileSystemItem.getProject();
    return ContainerUtil.findAll(getHelpers(), fileReferenceHelper -> fileReferenceHelper.isMine(project, file));
  }

  public static boolean areElementsEquivalent(@Nonnull final PsiFileSystemItem element1, @Nonnull final PsiFileSystemItem element2) {
    return element2.getManager().areElementsEquivalent(element1, element2);
  }
}
