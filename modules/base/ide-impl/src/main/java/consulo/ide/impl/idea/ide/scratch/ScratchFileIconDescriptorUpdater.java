/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.scratch.RootType;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
@ExtensionImpl
public class ScratchFileIconDescriptorUpdater implements IconDescriptorUpdater {
  private ScratchFileService myScratchFileService;

  @Inject
  public ScratchFileIconDescriptorUpdater(ScratchFileService scratchFileService) {
    myScratchFileService = scratchFileService;
  }

  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    if (element instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile == null) {
        return;
      }
      RootType rootType = myScratchFileService.getRootType(virtualFile);
      if (rootType == null) return;
      iconDescriptor.setMainIcon(rootType.substituteIcon(element.getProject(), virtualFile));
    }
  }
}
