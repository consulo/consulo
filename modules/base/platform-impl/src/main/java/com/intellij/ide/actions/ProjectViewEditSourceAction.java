/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NavigatableWithText;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

// from kotlin
public class ProjectViewEditSourceAction extends BaseNavigateToSourceAction {
  public ProjectViewEditSourceAction() {
    super(true);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);

    Presentation presentation = e.getPresentation();
    if (!presentation.isVisible() || !presentation.isEnabled()) {
      return;
    }

    Navigatable[] navigatables = getNavigatables(e.getDataContext());
    if (navigatables == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    boolean find = Arrays.stream(navigatables)
            .map(it -> it instanceof NavigatableWithText ? ((NavigatableWithText)it).getNavigateActionText(true) : null)
            .filter(Objects::nonNull)
            .findAny()
            .isPresent();
    e.getPresentation().setEnabledAndVisible(find);
  }
}
