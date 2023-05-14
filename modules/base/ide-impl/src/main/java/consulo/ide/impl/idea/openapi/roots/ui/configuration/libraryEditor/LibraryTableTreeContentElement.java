/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.style.StandardColors;

import jakarta.annotation.Nullable;

public abstract class LibraryTableTreeContentElement<E> extends NodeDescriptor<E> {
  protected LibraryTableTreeContentElement(@Nullable NodeDescriptor parentDescriptor) {
    super(parentDescriptor);
  }

  protected static ColorValue getForegroundColor(boolean isValid) {
    return isValid ? TargetAWT.from(UIUtil.getListForeground()) : StandardColors.RED;
  }

  @RequiredUIAccess
  @Override
  public boolean update() {
    return false;
  }

  @Override
  public E getElement() {
    return (E)this;
  }
}
