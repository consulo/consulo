/*
 * Copyright 2013 Consulo.org
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
package com.intellij.ide;

import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:25/19.07.13
 */
public class IconDescriptorUpdaters {
  private static final IconDescriptorUpdater[] ourCache = IconDescriptorUpdater.EP_NAME.getExtensions();
  private static final Key<CachedValue<IconKey>> KEY = Key.create("icon-key");

  private static class IconKey implements ModificationTracker {
    private PsiElement myElement;
    private Icon myIcon;

    private IconKey(PsiElement element, Icon icon) {
      myElement = element;
      myIcon = icon;
    }

    @Override
    public long getModificationCount() {
      return PsiModificationTracker.SERVICE.getInstance(myElement.getProject()).getJavaStructureModificationCount();
    }
  }

  @NotNull
  public static Icon getIcon(@NotNull final PsiElement element, @Iconable.IconFlags final int flags) {
    CachedValue<IconKey> cachedValue = element.getUserData(KEY);
    if (cachedValue == null) {
      cachedValue = CachedValuesManager.getManager(element.getProject()).createCachedValue(new CachedValueProvider<IconKey>() {
        @Nullable
        @Override
        public Result<IconKey> compute() {
          return Result.createSingleDependency(new IconKey(element, getIconWithoutCache(element, flags)),
                                               PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
        }
      });
      element.putUserData(KEY, cachedValue);
    }
    IconKey value = cachedValue.getValue();
    return value.myIcon;
  }

  @NotNull
  public static Icon getIconWithoutCache(@NotNull PsiElement element, int flags) {
    IconDescriptor iconDescriptor = new IconDescriptor(null);
    for (IconDescriptorUpdater iconDescriptorUpdater : ourCache) {
      iconDescriptorUpdater.updateIcon(iconDescriptor, element, flags);
    }
    return iconDescriptor.toIcon();
  }

  public static void processExistingDescriptor(@NotNull IconDescriptor descriptor, @NotNull PsiElement element, int flags) {
    for (IconDescriptorUpdater iconDescriptorUpdater : ourCache) {
      iconDescriptorUpdater.updateIcon(descriptor, element, flags);
    }
  }
}
