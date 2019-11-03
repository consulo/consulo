/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.wm.impl.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * from kotlin
 */
public class TogglePopupHintsPanelProvider implements StatusBarWidgetProvider {
  @Nullable
  @Override
  public StatusBarWidget getWidget(@Nonnull Project project) {
    return new TogglePopupHintsPanel(project);
  }

  @Nonnull
  @Override
  public String getAnchor() {
    return StatusBar.Anchors.after(StatusBar.StandardWidgets.READONLY_ATTRIBUTE_PANEL);
  }
}
