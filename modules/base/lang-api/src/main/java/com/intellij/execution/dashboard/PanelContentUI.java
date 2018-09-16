/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.dashboard;

import com.intellij.ui.content.*;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * PanelContentUI simply shows selected content in a panel.
 *
 * @author konstantin.aleev
 */
class PanelContentUI implements ContentUI {
  private JPanel myPanel;

  @Override
  public JComponent getComponent() {
    initUI();
    return myPanel;
  }

  @Override
  public void setManager(@Nonnull ContentManager manager) {
    manager.addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        initUI();
        if (ContentManagerEvent.ContentOperation.add == event.getOperation()) {
          showContent(event.getContent());
        }
        else if (ContentManagerEvent.ContentOperation.remove == event.getOperation()) {
          hideContent();
        }
      }
    });
  }

  private void initUI() {
    if (myPanel != null) {
      return;
    }
    myPanel = new JPanel(new BorderLayout());
  }

  private void showContent(@Nonnull Content content) {
    if (myPanel.getComponentCount() != 1 ||
        myPanel.getComponent(0) != content.getComponent()) {
      myPanel.removeAll();
      myPanel.add(content.getComponent(), BorderLayout.CENTER);

      myPanel.revalidate();
      myPanel.repaint();
    }
  }

  private void hideContent() {
    myPanel.removeAll();
    myPanel.revalidate();
    myPanel.repaint();
  }

  @Override
  public boolean isSingleSelection() {
    return true;
  }

  @Override
  public boolean isToSelectAddedContent() {
    return true;
  }

  @Override
  public boolean canBeEmptySelection() {
    return false;
  }

  @Override
  public void beforeDispose() {
  }

  @Override
  public boolean canChangeSelectionTo(@Nonnull Content content, boolean implicit) {
    return true;
  }

  @Nonnull
  @Override
  public String getCloseActionName() {
    return "";
  }

  @Nonnull
  @Override
  public String getCloseAllButThisActionName() {
    return "";
  }

  @Nonnull
  @Override
  public String getPreviousContentActionName() {
    return "";
  }

  @Nonnull
  @Override
  public String getNextContentActionName() {
    return "";
  }

  @Override
  public void dispose() {
  }
}
