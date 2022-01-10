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
package consulo.fileTypes.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.ui.IconDeferrer;
import com.intellij.util.AnyIconKey;
import com.intellij.util.BitUtil;
import com.intellij.util.NullableFunction;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdaters;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20-Nov-16.
 */
public class VfsIconUtil {
  private static final NullableFunction<AnyIconKey<VirtualFile>, Image> ourVirtualFileIconFunc = key -> {
    final VirtualFile file = key.getObject();
    final int flags = key.getFlags();
    Project project = key.getProject();

    if (!file.isValid() || project != null && (project.isDisposed() || !wasEverInitialized(project))) return null;

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
      iconDescriptor.addLayerIcon(AllIcons.Nodes.Symlink);
    }

    if (BitUtil.isSet(flags, Iconable.ICON_FLAG_READ_STATUS)) {
      final boolean isLocked = !file.isWritable() || !WritingAccessProvider.isPotentiallyWritable(file, project);
      if (isLocked) {
        iconDescriptor.addLayerIcon(AllIcons.Nodes.Locked);
      }
    }

    Image icon = iconDescriptor.toIcon();
    Iconable.LastComputedIcon.put(file, icon, flags);
    return icon;
  };

  private static final Key<Boolean> PROJECT_WAS_EVER_INITIALIZED = Key.create("iconDeferrer:projectWasEverInitialized");

  private static boolean wasEverInitialized(@Nonnull Project project) {
    Boolean was = project.getUserData(PROJECT_WAS_EVER_INITIALIZED);
    if (was == null) {
      if (project.isInitialized()) {
        was = Boolean.valueOf(true);
        project.putUserData(PROJECT_WAS_EVER_INITIALIZED, was);
      }
      else {
        was = Boolean.valueOf(false);
      }
    }

    return was.booleanValue();
  }

  @Nullable
  public static Image getIcon(@Nonnull final VirtualFile file, @Iconable.IconFlags final int flags, @Nullable final Project project) {
    Image icon = Iconable.LastComputedIcon.get(file, flags);
    if (icon == null) {
      icon = VirtualFilePresentation.getIcon(file);
    }

    return IconDeferrer.getInstance().defer(icon, new AnyIconKey<>(file, project, flags), ourVirtualFileIconFunc);
  }

  @Nullable
  public static Image getIconNoDefer(@Nonnull final VirtualFile file, @Iconable.IconFlags final int flags, @Nullable final Project project) {
    if(project == null || !wasEverInitialized(project)) {
      return VirtualFilePresentation.getIcon(file);
    }
    return ourVirtualFileIconFunc.apply(new AnyIconKey<>(file, project, flags));
  }
}
