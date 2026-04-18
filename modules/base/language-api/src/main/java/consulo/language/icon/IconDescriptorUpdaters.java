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
import consulo.component.util.Iconable;
import consulo.language.psi.*;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2013-07-19
 */
public final class IconDescriptorUpdaters {
    private static final Supplier<Image> ourVisibilityIconPlaceholder =
        LazyValue.notNull(() -> Image.empty(PlatformIconGroup.nodesC_public().getWidth(), PlatformIconGroup.nodesC_public().getHeight()));

    private static final Function<ElementIconRequest, @Nullable Image> ICON_COMPUTE = request -> {
        PsiElement element = request.myPointer.getElement();
        if (element == null || !element.isValid() || element.getProject().isDisposed()) {
            return null;
        }

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
        public boolean equals(@Nullable Object o) {
            return this == o
                || o instanceof ElementIconRequest that
                && myFlags == that.myFlags
                && myPointer.equals(that.myPointer);
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

    @RequiredReadAction
    public static Image getIcon(PsiElement element, @Iconable.IconFlags int flags) {
        if (!element.isValid()) {
            return PlatformIconGroup.nodesNodeplaceholder();
        }

        Image baseIcon = Iconable.LastComputedIcon.get(element, flags);
        if (baseIcon == null) {
            baseIcon = computeBaseIcon(element, flags);
        }
        return IconDeferrer.getInstance().defer(baseIcon, new ElementIconRequest(element, flags), ICON_COMPUTE);
    }

    private static Image computeBaseIcon(PsiElement element, int flags) {
        Image icon = computeBaseIcon(element);
        if ((flags & Iconable.ICON_FLAG_VISIBILITY) > 0) {
            return ImageEffects.appendRight(icon, ourVisibilityIconPlaceholder.get());
        }
        return icon;
    }

    private static Image computeBaseIcon(PsiElement element) {
        if (element instanceof PsiFileSystemItem fileSystemItem) {
            VirtualFile file = fileSystemItem.getVirtualFile();
            if (file != null) {
                return VirtualFileManager.getInstance().getBaseFileIcon(file);
            }
            return PlatformIconGroup.nodesNodeplaceholder();
        }

        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null) {
            VirtualFile virtualFile = containingFile.getVirtualFile();
            if (virtualFile != null) {
                return virtualFile.getFileType().getIcon();
            }
        }
        return PlatformIconGroup.nodesNodeplaceholder();
    }

    @RequiredReadAction
    public static Image getIconWithoutCache(PsiElement element, int flags) {
        IconDescriptor iconDescriptor = new IconDescriptor(null);
        element
            .getProject()
            .getExtensionPoint(IconDescriptorUpdater.class).forEachExtensionSafe(it -> it.updateIcon(iconDescriptor, element, flags));
        return iconDescriptor.toIcon();
    }

    @RequiredReadAction
    public static void processExistingDescriptor(IconDescriptor iconDescriptor, PsiElement element, int flags) {
        element
            .getProject()
            .getExtensionPoint(IconDescriptorUpdater.class).forEachExtensionSafe(it -> it.updateIcon(iconDescriptor, element, flags));
    }
}
