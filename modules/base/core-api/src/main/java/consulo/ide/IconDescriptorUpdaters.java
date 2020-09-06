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
import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.IconDeferrer;
import com.intellij.util.NullableFunction;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 0:25/19.07.13
 */
public class IconDescriptorUpdaters {
  private static final NotNullLazyValue<Image> ourVisibilityIconPlaceholder =
          NotNullLazyValue.createValue(() -> Image.empty(AllIcons.Nodes.C_public.getWidth(), AllIcons.Nodes.C_public.getHeight()));

  private static final NullableFunction<ElementIconRequest, Image> ourIconCompute = request -> {
    final PsiElement element = request.myPointer.getElement();
    if (element == null || !element.isValid() || element.getProject().isDisposed()) return null;

    Image icon = getIconWithoutCache(element, request.myFlags);
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

  @Nonnull
  @RequiredReadAction
  public static Image getIcon(@Nonnull final PsiElement element, @Iconable.IconFlags final int flags) {
    if (!element.isValid()) return AllIcons.Nodes.NodePlaceholder;

    Image baseIcon = Iconable.LastComputedIcon.get(element, flags);
    if (baseIcon == null) {
      baseIcon = computeBaseIcon(element, flags);
    }
    return IconDeferrer.getInstance().defer(baseIcon, new ElementIconRequest(element, flags), ourIconCompute);
  }

  @Nonnull
  private static Image computeBaseIcon(@Nonnull PsiElement element, int flags) {
    Image icon = computeBaseIcon(element);
    if ((flags & Iconable.ICON_FLAG_VISIBILITY) > 0) {
      return ImageEffects.appendRight(icon, ourVisibilityIconPlaceholder.getValue());
    }
    return icon;
  }

  @Nonnull
  private static Image computeBaseIcon(@Nonnull PsiElement element) {
    if(element instanceof PsiFileSystemItem) {
      VirtualFile file = ((PsiFileSystemItem)element).getVirtualFile();
      if(file != null) {
        return VirtualFilePresentation.getIcon(file);
      }
      return AllIcons.Nodes.NodePlaceholder;
    }

    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      VirtualFile virtualFile = containingFile.getVirtualFile();
      if (virtualFile != null) {
        return virtualFile.getFileType().getIcon();
      }
    }
    return AllIcons.Nodes.NodePlaceholder;
  }

  @Nonnull
  @RequiredReadAction
  public static Image getIconWithoutCache(@Nonnull PsiElement element, int flags) {
    Project project = element.getProject();
    IconDescriptor iconDescriptor = new IconDescriptor(null);
    IconDescriptorUpdater.EP_NAME.composite(project).updateIcon(iconDescriptor, element, flags);
    return iconDescriptor.toIcon();
  }

  @RequiredReadAction
  public static void processExistingDescriptor(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    Project project = element.getProject();
    IconDescriptorUpdater.EP_NAME.composite(project).updateIcon(iconDescriptor, element, flags);
  }
}
