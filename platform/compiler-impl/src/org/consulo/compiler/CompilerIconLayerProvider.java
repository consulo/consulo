/*
 * Copyright 2013 Consulo.org
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
package org.consulo.compiler;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.IconLayerProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 20:18/25.05.13
 */
public class CompilerIconLayerProvider implements IconLayerProvider {
  @Override
  public Icon getLayerIcon(@NotNull Iconable element, boolean isLocked) {
    VirtualFile vFile = null;
    Project project = null;
    if (element instanceof PsiElement) {
      project = ((PsiElement) element).getProject();
      final PsiFile containingFile = ((PsiElement) element).getContainingFile();
      vFile = containingFile == null ? null : containingFile.getVirtualFile();
    }

    if (vFile != null && isExcluded(vFile, project)) {
      return PlatformIcons.EXCLUDED_FROM_COMPILE_ICON;
    }
    return null;
  }

  @Override
  public String getLayerDescription() {
    return CodeInsightBundle.message("node.excluded.flag.tooltip");
  }

  public static boolean isExcluded(final VirtualFile vFile, final Project project) {
    return false/*vFile != null
           && FileIndexFacade.getInstance(project).isInSource(vFile)
           && CompilerConfiguration.getInstance(project).isExcludedFromCompilation(vFile)*/;
  }
}
