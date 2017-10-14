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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowType;
import consulo.ui.ex.ToolWindowInternalDecorator;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * @author Vladimir Kondratyev
 */
public interface InternalDecoratorListener extends EventListener {
  void anchorChanged(@NotNull ToolWindowInternalDecorator source, @NotNull ToolWindowAnchor anchor);

  void autoHideChanged(@NotNull ToolWindowInternalDecorator source, boolean autoHide);

  void hidden(@NotNull ToolWindowInternalDecorator source);

  void hiddenSide(@NotNull ToolWindowInternalDecorator source);

  void resized(@NotNull ToolWindowInternalDecorator source);

  void activated(@NotNull ToolWindowInternalDecorator source);

  void typeChanged(@NotNull ToolWindowInternalDecorator source, @NotNull ToolWindowType type);

  void sideStatusChanged(@NotNull ToolWindowInternalDecorator source, boolean isSideTool);

  void contentUiTypeChanges(@NotNull ToolWindowInternalDecorator sources, @NotNull ToolWindowContentUiType type);

  void visibleStripeButtonChanged(@NotNull ToolWindowInternalDecorator source, boolean visible);
}
