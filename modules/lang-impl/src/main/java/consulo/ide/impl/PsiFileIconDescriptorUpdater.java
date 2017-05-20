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
package consulo.ide.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.NativeFileIconUtil;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import consulo.annotations.RequiredReadAction;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;
import consulo.lang.LanguageElementIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:28/19.07.13
 */
public class PsiFileIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof PsiFile) {
      if (iconDescriptor.getMainIcon() == null) {
        VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
        if (virtualFile != null) {
          iconDescriptor.setMainIcon(NativeFileIconUtil.INSTANCE.getIcon(virtualFile));
        }

        if (iconDescriptor.getMainIcon() == null) {
          FileType fileType = ((PsiFile)element).getFileType();
          iconDescriptor.setMainIcon(fileType.getIcon());
        }
      }

      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null && virtualFile.is(VFileProperty.SYMLINK)) {
        iconDescriptor.addLayerIcon(AllIcons.Nodes.Symlink);
      }
    }
    else {
      Icon languageElementIcon = LanguageElementIcons.INSTANCE.forLanguage(element.getLanguage());
      if (languageElementIcon == null) {
        return;
      }

      iconDescriptor.addLayerIcon(languageElementIcon);
    }
  }
}
