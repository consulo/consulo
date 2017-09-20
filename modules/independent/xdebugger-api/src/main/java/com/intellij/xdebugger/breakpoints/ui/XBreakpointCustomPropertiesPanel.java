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
package com.intellij.xdebugger.breakpoints.ui;

import com.intellij.openapi.Disposable;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import consulo.annotations.DeprecationInfo;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.migration.ToSwingWrappers;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author nik
 */
public abstract class XBreakpointCustomPropertiesPanel<B extends XBreakpoint<?>> implements Disposable {

  @NotNull
  @Deprecated
  @DeprecationInfo("Please implement interface via new UI API")
  public JComponent getComponent() {
    return (JComponent)ToSwingWrappers.toAWT(getUIComponent());
  }

  @NotNull
  @RequiredUIAccess
  public Component getUIComponent() {
    throw new AbstractMethodError();
  }

  @RequiredUIAccess
  public abstract void saveTo(@NotNull B breakpoint);

  @RequiredUIAccess
  public abstract void loadFrom(@NotNull B breakpoint);

  @Override
  public void dispose() {
  }

  public boolean isVisibleOnPopup(@NotNull B breakpoint) {
    return true;
  }
}
