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
import javax.annotation.Nonnull;

import java.util.EventListener;

/**
 * @author Vladimir Kondratyev
 */
public interface InternalDecoratorListener extends EventListener {
  void anchorChanged(@Nonnull ToolWindowInternalDecorator source, @Nonnull ToolWindowAnchor anchor);

  void autoHideChanged(@Nonnull ToolWindowInternalDecorator source, boolean autoHide);

  void hidden(@Nonnull ToolWindowInternalDecorator source);

  void hiddenSide(@Nonnull ToolWindowInternalDecorator source);

  void resized(@Nonnull ToolWindowInternalDecorator source);

  void activated(@Nonnull ToolWindowInternalDecorator source);

  void typeChanged(@Nonnull ToolWindowInternalDecorator source, @Nonnull ToolWindowType type);

  void sideStatusChanged(@Nonnull ToolWindowInternalDecorator source, boolean isSideTool);

  void contentUiTypeChanges(@Nonnull ToolWindowInternalDecorator sources, @Nonnull ToolWindowContentUiType type);

  void visibleStripeButtonChanged(@Nonnull ToolWindowInternalDecorator source, boolean visible);
}
