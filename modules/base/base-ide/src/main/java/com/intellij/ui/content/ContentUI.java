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
package com.intellij.ui.content;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import javax.annotation.Nonnull;

public interface ContentUI extends Disposable {
  void setManager(@Nonnull ContentManager manager);

  boolean isSingleSelection();

  boolean isToSelectAddedContent();

  boolean canBeEmptySelection();

  void beforeDispose();

  boolean canChangeSelectionTo(@Nonnull Content content, boolean implicit);

  @Nonnull
  String getCloseActionName();

  @Nonnull
  String getCloseAllButThisActionName();

  @Nonnull
  String getPreviousContentActionName();

  @Nonnull
  String getNextContentActionName();

  @Nonnull
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  // TODO [VISTALL] AWT & Swing dependency
  // region AWT & Swing dependency
  @Deprecated
  @DeprecationInfo("Use #getUIComponent")
  default javax.swing.JComponent getComponent() {
    throw new AbstractMethodError();
  }
  // endregion
}
