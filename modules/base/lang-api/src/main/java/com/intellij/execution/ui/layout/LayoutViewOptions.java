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

package com.intellij.execution.ui.layout;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.ui.content.Content;
import javax.annotation.Nonnull;

public interface LayoutViewOptions {

  String STARTUP = "startup";

  @Nonnull
  LayoutViewOptions setTopToolbar(@Nonnull ActionGroup actions, @Nonnull String place);

  @Nonnull
  LayoutViewOptions setLeftToolbar(@Nonnull ActionGroup leftToolbar, @Nonnull String place);

  @Nonnull
  LayoutViewOptions setMinimizeActionEnabled(boolean enabled);

  @Nonnull
  LayoutViewOptions setMoveToGridActionEnabled(boolean enabled);

  @Nonnull
  LayoutViewOptions setAttractionPolicy(@Nonnull String contentId, LayoutAttractionPolicy policy);

  @Nonnull
  LayoutViewOptions setConditionAttractionPolicy(@Nonnull String condition, LayoutAttractionPolicy policy);

  boolean isToFocus(@Nonnull Content content, @Nonnull String condition);

  @Nonnull
  LayoutViewOptions setToFocus(@javax.annotation.Nullable Content content, @Nonnull String condition);

  AnAction getLayoutActions();
  @Nonnull
  AnAction[] getLayoutActionsList();

  @Nonnull
  LayoutViewOptions setTabPopupActions(@Nonnull ActionGroup group);
  @Nonnull
  LayoutViewOptions setAdditionalFocusActions(@Nonnull ActionGroup group);

  AnAction getSettingsActions();
  @Nonnull
  AnAction[] getSettingsActionsList();
}