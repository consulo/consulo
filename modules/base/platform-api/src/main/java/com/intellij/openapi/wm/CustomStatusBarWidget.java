/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.wm;

import consulo.awt.TargetAWT;
import consulo.ui.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * User: spLeaner
 */
public interface CustomStatusBarWidget extends StatusBarWidget {
  @Nonnull
  default JComponent getComponent() {
    Component uiComponent = getUIComponent();
    if (uiComponent != null) {
      return (JComponent)TargetAWT.to(uiComponent);
    }

    throw new AbstractMethodError();
  }

  @Nullable
  default Component getUIComponent() {
    // override isUnified() too
    return null;
  }

  default boolean isUnified() {
    return false;
  }
}
