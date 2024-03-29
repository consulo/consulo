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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.psi.PsiElement;

import consulo.ui.ex.awt.event.ColorPickerListener;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * User: ksafonov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ColorPickerListenerFactory {
  private static final ExtensionPointName<ColorPickerListenerFactory> EP_NAME = ExtensionPointName.create(ColorPickerListenerFactory.class);

  public static ColorPickerListener[] createListenersFor(@Nullable final PsiElement element) {
    final List<ColorPickerListener> listeners = ContainerUtil.mapNotNull(EP_NAME.getExtensionList(), factory -> factory.createListener(element));
    return listeners.toArray(new ColorPickerListener[listeners.size()]);
  }

  public abstract ColorPickerListener createListener(@Nullable PsiElement element);
}
