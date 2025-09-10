/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.navigation.NavigationUtil;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nullable;

public class EditSourceUtil {
  private EditSourceUtil() {
  }

  @Nullable
  @RequiredReadAction
  public static Navigatable getDescriptor(PsiElement element) {
    if (!canNavigate(element)) {
      return null;
    }
    if (element instanceof PomTargetPsiElement) {
      return ((PomTargetPsiElement)element).getTarget();
    }
    PsiElement navigationElement = element.getNavigationElement();
    if (navigationElement instanceof PomTargetPsiElement) {
      return ((PomTargetPsiElement)navigationElement).getTarget();
    }
    int offset = navigationElement instanceof PsiFile ? -1 : navigationElement.getTextOffset();
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(navigationElement);
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }

    OpenFileDescriptorFactory.Builder builder = OpenFileDescriptorFactory.getInstance(navigationElement.getProject()).builder(virtualFile);
    builder.offset(offset);
    builder.useCurrentWindow(NavigationUtil.USE_CURRENT_WINDOW.isIn(navigationElement));
    return builder.build();
  }

  @RequiredReadAction
  public static boolean canNavigate(PsiElement element) {
    if (element == null || !element.isValid()) {
      return false;
    }

    VirtualFile file = PsiUtilCore.getVirtualFile(element.getNavigationElement());
    return file != null && file.isValid() && !file.is(VFileProperty.SPECIAL) && !VirtualFileUtil.isBrokenLink(file);
  }

  public static void navigate(NavigationItem item, boolean requestFocus, boolean useCurrentWindow) {
    NavigationUtil.navigate(item, requestFocus, useCurrentWindow);
  }
}