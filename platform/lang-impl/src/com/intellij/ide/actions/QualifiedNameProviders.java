/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.ide.actions;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.LogicalRoot;
import com.intellij.util.LogicalRootsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14.03.14
 */
public class QualifiedNameProviders {
  @Nullable
  public static String elementToFqn(final PsiElement element) {
    return elementToFqn(element, null);
  }

  @Nullable
  public static String elementToFqn(final PsiElement element, @Nullable Editor editor) {
    String result = QualifiedNameProviders.getQualifiedNameFromProviders(element);
    if (result != null) return result;

    if (editor != null) { //IDEA-70346
      PsiReference reference = TargetElementUtilBase.findReference(editor, editor.getCaretModel().getOffset());
      if (reference != null) {
        result = QualifiedNameProviders.getQualifiedNameFromProviders(reference.resolve());
        if (result != null) return result;
      }
    }

    String fqn = null;
    if (element instanceof PsiFile) {
      final PsiFile file = (PsiFile)element;
      fqn = FileUtil.toSystemIndependentName(getFileFqn(file));
    }
    return fqn;
  }

  @Nullable
  public static String getQualifiedNameFromProviders(@Nullable com.intellij.psi.PsiElement element) {
    if (element == null) return null;
    for (QualifiedNameProvider provider : Extensions.getExtensions(QualifiedNameProvider.EP_NAME)) {
      String result = provider.getQualifiedName(element);
      if (result != null) return result;
    }
    return null;
  }

  @Nullable
  public static Pair<PsiElement, QualifiedNameProvider> findElementByQualifiedName(@Nullable String qName, @NotNull Project project) {
    QualifiedNameProvider theProvider = null;
    PsiElement element = null;
    for(QualifiedNameProvider provider: QualifiedNameProvider.EP_NAME.getExtensions()) {
      element = provider.qualifiedNameToElement(qName, project);
      if (element != null) {
        theProvider = provider;
        break;
      }
    }
    return theProvider == null ? null : new Pair<PsiElement, QualifiedNameProvider>(element, theProvider);
  }

  @NotNull
  public static String getFileFqn(final PsiFile file) {
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return file.getName();
    }
    final Project project = file.getProject();
    final LogicalRoot logicalRoot = LogicalRootsManager.getLogicalRootsManager(project).findLogicalRoot(virtualFile);
    if (logicalRoot != null) {
      String logical = FileUtil.toSystemIndependentName(VfsUtil.virtualToIoFile(logicalRoot.getVirtualFile()).getPath());
      String path = FileUtil.toSystemIndependentName(VfsUtil.virtualToIoFile(virtualFile).getPath());
      return "/" + FileUtil.getRelativePath(logical, path, '/');
    }

    final VirtualFile contentRoot = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(virtualFile);
    if (contentRoot != null) {
      return "/" + FileUtil.getRelativePath(VfsUtil.virtualToIoFile(contentRoot), VfsUtil.virtualToIoFile(virtualFile));
    }
    return virtualFile.getPath();
  }
}
