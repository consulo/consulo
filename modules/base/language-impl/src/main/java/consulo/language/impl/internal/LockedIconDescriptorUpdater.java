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
package consulo.language.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.component.util.Iconable;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.util.lang.BitUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.annotation.access.RequiredReadAction;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2013-07-19
 */
@ExtensionImpl
public class LockedIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    if (BitUtil.isSet(flags, Iconable.ICON_FLAG_READ_STATUS)) {
      VirtualFile file = PsiUtilCore.getVirtualFile(element);
      final boolean isLocked = !element.isWritable() || file != null && !WritingAccessProvider.isPotentiallyWritable(file, element.getProject());

      if (isLocked) {
        iconDescriptor.addLayerIcon(AllIcons.Nodes.Locked);
      }
    }
  }
}
