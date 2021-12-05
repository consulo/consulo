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
package com.intellij.ui.tabs.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.impl.singleRow.CompressibleSingleRowLayout;
import com.intellij.ui.tabs.impl.singleRow.ScrollableSingleRowLayout;
import com.intellij.ui.tabs.impl.singleRow.SingleRowLayout;
import consulo.disposer.Disposable;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author pegov
 */
public class JBEditorTabs extends JBTabsImpl {
  public JBEditorTabs(@Nullable Project project, ActionManager actionManager, IdeFocusManager focusManager, @Nullable Disposable parent) {
    super(project, actionManager, focusManager, parent, true);
  }

  @Override
  protected SingleRowLayout createSingleRowLayout() {
    if (!UISettings.getInstance().HIDE_TABS_IF_NEED && supportsCompression()) {
      return new CompressibleSingleRowLayout(this);
    }
    else {
      return new ScrollableSingleRowLayout(this);
    }
  }

  @Override
  public boolean supportsCompression() {
    return true;
  }

  @Nullable
  public Rectangle getSelectedBounds() {
    TabLabel label = getSelectedLabel();
    return label != null ? label.getBounds() : null;
  }

  @Override
  public boolean useSmallLabels() {
    return UISettings.getInstance().USE_SMALL_LABELS_ON_TABS;
  }

  @Override
  public boolean isAlphabeticalMode() {
    return UISettings.getInstance().EDITOR_TABS_ALPHABETICAL_SORT;
  }
}
