/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler;

import com.intellij.icons.AllIcons;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import consulo.annotation.access.RequiredReadAction;

/**
 * @author VISTALL
 * @since 1:20/19.07.13
 */
public class CompilerIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    Project project = element.getProject();
    final PsiFile containingFile = element.getContainingFile();
    VirtualFile vFile = containingFile == null ? null : containingFile.getVirtualFile();

    if (vFile != null && isExcluded(vFile, project)) {
      iconDescriptor.addLayerIcon(AllIcons.Nodes.ExcludedFromCompile);
    }
  }

  public static boolean isExcluded(final VirtualFile vFile, final Project project) {
    return vFile != null &&
           FileIndexFacade.getInstance(project).isInSource(vFile) &&
           CompilerManager.getInstance(project).isExcludedFromCompilation(vFile);
  }
}
