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
package consulo.ide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.ui.IconDeferrer;
import com.intellij.ui.RowIcon;
import com.intellij.util.NullableFunction;
import com.intellij.util.ui.EmptyIcon;
import consulo.annotations.RequiredReadAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:25/19.07.13
 */
public class IconDescriptorUpdaters {
  private static final NotNullLazyValue<Icon> ourVisibilityIconPlaceholder = NotNullLazyValue.createValue(() -> EmptyIcon.create(AllIcons.Nodes.C_public));

  private static final NullableFunction<ElementIconRequest, Icon> ourIconCompute = request -> {
    final PsiElement element = request.myPointer.getElement();
    if (element == null || !element.isValid() || element.getProject().isDisposed()) return null;

    Icon icon = getIconWithoutCache(element, request.myFlags);
    Iconable.LastComputedIcon.put(element, icon, request.myFlags);
    return icon;
  };

  private static class ElementIconRequest {
    private final SmartPsiElementPointer<?> myPointer;
    @Iconable.IconFlags
    private final int myFlags;

    public ElementIconRequest(PsiElement element, @Iconable.IconFlags int flags) {
      myPointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
      myFlags = flags;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ElementIconRequest)) return false;

      ElementIconRequest request = (ElementIconRequest)o;

      if (myFlags != request.myFlags) return false;
      if (!myPointer.equals(request.myPointer)) return false;

      return true;
    }

    @Override
    public String toString() {
      return myPointer.toString() + "/" + myFlags;
    }

    @Override
    public int hashCode() {
      int result = myPointer.hashCode();
      result = 31 * result + myFlags;
      return result;
    }
  }

  @NotNull
  @RequiredReadAction
  public static Icon getIcon(@NotNull final PsiElement element, @Iconable.IconFlags final int flags) {
    if (!element.isValid()) return AllIcons.Nodes.NodePlaceholder;

    Icon baseIcon = Iconable.LastComputedIcon.get(element, flags);
    if (baseIcon == null) {
      baseIcon = computeBaseIcon(element, flags);
    }
    return IconDeferrer.getInstance().defer(baseIcon, new ElementIconRequest(element, flags), ourIconCompute);
  }

  @NotNull
  private static Icon computeBaseIcon(@NotNull PsiElement element, int flags) {
    Icon icon = computeBaseIcon(element);
    if ((flags & Iconable.ICON_FLAG_VISIBILITY) > 0) {
      return new RowIcon(icon, ourVisibilityIconPlaceholder.getValue());
    }
    return icon;
  }

  @NotNull
  private static Icon computeBaseIcon(@NotNull PsiElement element) {
    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      VirtualFile virtualFile = containingFile.getVirtualFile();
      if (virtualFile != null) {
        Icon icon = virtualFile.getFileType().getIcon();
        if (icon != null) {
          return icon;
        }
      }
    }
    return AllIcons.Nodes.NodePlaceholder;
  }

  @NotNull
  @RequiredReadAction
  public static Icon getIconWithoutCache(@NotNull PsiElement element, int flags) {
    IconDescriptor iconDescriptor = new IconDescriptor(null);
    IconDescriptorUpdater.EP_NAME.composite().updateIcon(iconDescriptor, element, flags);
    return iconDescriptor.toIcon();
  }

  @RequiredReadAction
  public static void processExistingDescriptor(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    IconDescriptorUpdater.EP_NAME.composite().updateIcon(iconDescriptor, element, flags);
  }
}
