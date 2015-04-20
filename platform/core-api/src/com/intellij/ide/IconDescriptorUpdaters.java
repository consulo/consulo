/*
 * Copyright 2013 must-be.org
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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:25/19.07.13
 */
public class IconDescriptorUpdaters {
  @NotNull
  public static Icon getIcon(@NotNull final PsiElement element, @Iconable.IconFlags final int flags) {
    Icon icon = Iconable.LastComputedIcon.get(element, flags);
    if (icon == null) {
      icon = getIconWithoutCache(element, flags);

      Iconable.LastComputedIcon.put(element, icon, flags);
    }
    return icon;
  }

  @NotNull
  public static Icon getIconWithoutCache(@NotNull PsiElement element, int flags) {
    IconDescriptor iconDescriptor = new IconDescriptor(null);
    IconDescriptorUpdater.EP_NAME.composite().updateIcon(iconDescriptor, element, flags);
    return iconDescriptor.toIcon();
  }

  public static void processExistingDescriptor(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    IconDescriptorUpdater.EP_NAME.composite().updateIcon(iconDescriptor, element, flags);
  }
}
