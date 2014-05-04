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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IconDeferrer;
import com.intellij.util.AnyIconKey;
import com.intellij.util.Function;
import org.consulo.lombok.annotations.LazyInstance;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:25/19.07.13
 */
public class IconDescriptorUpdaters {
  private static Function<AnyIconKey<PsiElement>, Icon> ourIconFunc = new Function<AnyIconKey<PsiElement>, Icon>() {
    @Override
    public Icon fun(AnyIconKey<PsiElement> psiElementAnyIconKey) {
      return getIconWithoutCache(psiElementAnyIconKey.getObject(), psiElementAnyIconKey.getFlags());
    }
  };

  @LazyInstance
  @NotNull
  private static IconDescriptorUpdater[] values() {
    return IconDescriptorUpdater.EP_NAME.getExtensions();
  }

  @NotNull
  public static Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
    return IconDeferrer.getInstance()
            .deferAutoUpdatable(AllIcons.Nodes.EmptyNode, new AnyIconKey<PsiElement>(element, element.getProject(), flags), ourIconFunc);
  }

  @NotNull
  public static Icon getIconWithoutCache(@NotNull PsiElement element, int flags) {
    IconDescriptor iconDescriptor = new IconDescriptor(null);
    for (IconDescriptorUpdater iconDescriptorUpdater : values()) {
      iconDescriptorUpdater.updateIcon(iconDescriptor, element, flags);
    }
    return iconDescriptor.toIcon();
  }

  public static void processExistingDescriptor(@NotNull IconDescriptor descriptor, @NotNull PsiElement element, int flags) {
    for (IconDescriptorUpdater iconDescriptorUpdater : values()) {
      iconDescriptorUpdater.updateIcon(descriptor, element, flags);
    }
  }
}
