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
package consulo.language.icon;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.component.util.Iconable;
import consulo.language.psi.*;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 0:25/19.07.13
 */
public final class IconDescriptorUpdaters {
  private static final Supplier<Image> ourVisibilityIconPlaceholder =
          LazyValue.notNull(() -> Image.empty(AllIcons.Nodes.C_public.getWidth(), AllIcons.Nodes.C_public.getHeight()));

  private static final Function<ElementIconRequest, Image> ourIconCompute = request -> {
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
      return ImageEffects.appendRight(icon, ourVisibilityIconPlaceholder.get());
    }
    return icon;
  }

  @Nonnull
  private static Image computeBaseIcon(@Nonnull PsiElement element) {
    if(element instanceof PsiFileSystemItem) {
      VirtualFile file = ((PsiFileSystemItem)element).getVirtualFile();
      if(file != null) {
        return VirtualFileManager.getInstance().getBaseFileIcon(file);
      }
      return PlatformIconGroup.nodesNodePlaceholder();
    }

    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      VirtualFile virtualFile = containingFile.getVirtualFile();
      if (virtualFile != null) {
        return virtualFile.getFileType().getIcon();
      }
    }
    return PlatformIconGroup.nodesNodePlaceholder();
  }

  @Nonnull
  @RequiredReadAction
  public static Image getIconWithoutCache(@Nonnull PsiElement element, int flags) {
    IconDescriptor iconDescriptor = new IconDescriptor(null);
    IconDescriptorUpdater.EP.forEachExtensionSafe(element.getProject(), it -> it.updateIcon(iconDescriptor, element, flags));
    return iconDescriptor.toIcon();
  }

  @RequiredReadAction
  public static void processExistingDescriptor(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    IconDescriptorUpdater.EP.forEachExtensionSafe(element.getProject(), it -> it.updateIcon(iconDescriptor, element, flags));
  }
}
