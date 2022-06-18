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
package consulo.ide.impl.compiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.compiler.CompilerManager;
import consulo.project.Project;
import consulo.language.content.FileIndexFacade;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 1:20/19.07.13
 */
@ExtensionImpl
public class CompilerIconDescriptorUpdater implements IconDescriptorUpdater {
  private final FileIndexFacade myFileIndexFacade;
  private final CompilerManager myCompilerManager;

  @Inject
  public CompilerIconDescriptorUpdater(FileIndexFacade fileIndexFacade, CompilerManager compilerManager) {
    myFileIndexFacade = fileIndexFacade;
    myCompilerManager = compilerManager;
  }

  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    VirtualFile vFile = PsiUtilCore.getVirtualFile(element);

    if (vFile != null && myFileIndexFacade.isInSource(vFile) && myCompilerManager.isExcludedFromCompilation(vFile)) {
      iconDescriptor.addLayerIcon(AllIcons.Nodes.ExcludedFromCompile);
    }
  }

  @Deprecated
  public static boolean isExcluded(final VirtualFile vFile, final Project project) {
    return vFile != null &&
           FileIndexFacade.getInstance(project).isInSource(vFile) &&
           CompilerManager.getInstance(project).isExcludedFromCompilation(vFile);
  }
}
