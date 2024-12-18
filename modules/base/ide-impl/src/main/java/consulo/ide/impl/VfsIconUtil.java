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

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.Iconable;
import consulo.ide.impl.idea.util.AnyIconKey;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import consulo.virtualFileSystem.WritingAccessProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20-Nov-16.
 */
public class VfsIconUtil {
    private static final Key<Boolean> PROJECT_WAS_EVER_INITIALIZED = Key.create("iconDeferrer:projectWasEverInitialized");

    private static boolean wasEverInitialized(@Nonnull Project project) {
        Boolean was = project.getUserData(PROJECT_WAS_EVER_INITIALIZED);
        if (was == null) {
            if (project.isInitialized()) {
                was = Boolean.TRUE;
                project.putUserData(PROJECT_WAS_EVER_INITIALIZED, was);
            }
            else {
                was = Boolean.FALSE;
            }
        }

        return was;
    }

    @RequiredReadAction
    private static Image requestIcon(Project project, VirtualFile file, int flags) {
        if (!file.isValid() || project != null && (project.isDisposed() || !wasEverInitialized(project))) {
            return null;
        }

        boolean processedDescriptors = false;
        // disable on webservice native icon
        IconDescriptor iconDescriptor = new IconDescriptor(VirtualFilePresentation.getIcon(file));

        if (project != null) {
            PsiManager manager = PsiManager.getInstance(project);
            final PsiElement element = file.isDirectory() ? manager.findDirectory(file) : manager.findFile(file);
            if (element != null) {
                IconDescriptorUpdaters.processExistingDescriptor(iconDescriptor, element, flags);
                processedDescriptors = true;
            }
        }

        // if descriptors not processed - we need add layer icon obviously
        if (!processedDescriptors && file.is(VFileProperty.SYMLINK)) {
            iconDescriptor.addLayerIcon(PlatformIconGroup.nodesSymlink());
        }

        if (BitUtil.isSet(flags, Iconable.ICON_FLAG_READ_STATUS)) {
            final boolean isLocked = !file.isWritable() || !WritingAccessProvider.isPotentiallyWritable(file, project);
            if (isLocked) {
                iconDescriptor.addLayerIcon(PlatformIconGroup.nodesLocked());
            }
        }

        Image icon = iconDescriptor.toIcon();
        Iconable.LastComputedIcon.put(file, icon, flags);
        return icon;
    }

    @Nullable
    public static Image getIcon(@Nonnull final VirtualFile file, @Iconable.IconFlags final int flags, @Nullable final Project project) {
        Image icon = Iconable.LastComputedIcon.get(file, flags);
        if (icon == null) {
            icon = VirtualFilePresentation.getIcon(file);
        }

        return IconDeferrer.getInstance().defer(icon, new AnyIconKey<>(file, project, flags), k -> requestIcon(k.getProject(), k.getObject(), k.getFlags()));
    }

    @Nonnull
    @RequiredReadAction
    public static Image getIconNoDefer(@Nonnull final VirtualFile file, @Iconable.IconFlags final int flags, @Nullable final Project project) {
        UIAccess.assetIsNotUIThread();

        Image image = requestIcon(project, file, flags);
        if (image == null) {
            return Image.empty(Image.DEFAULT_ICON_SIZE);
        }
        return image;
    }
}
