/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.diff;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.diff.editor.DiffVirtualFile;
import consulo.language.psi.PsiBinaryFile;
import consulo.language.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24/09/2021
 */
@ExtensionImpl
public class DiffIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    if(element instanceof PsiBinaryFile binaryFile && binaryFile.getVirtualFile() instanceof DiffVirtualFile) {
      iconDescriptor.setMainIcon(PlatformIconGroup.actionsDiff());
      iconDescriptor.setRightIcon(null);
    }
  }
}
