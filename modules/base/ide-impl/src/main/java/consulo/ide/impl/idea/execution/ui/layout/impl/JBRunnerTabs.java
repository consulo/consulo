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
package consulo.ide.impl.idea.execution.ui.layout.impl;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.tabs.impl.JBEditorTabs;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.tab.TabInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Map;

/**
 * @author Dennis.Ushakov
 */
public class JBRunnerTabs extends JBEditorTabs {
  public JBRunnerTabs(@Nullable Project project, ActionManager actionManager, IdeFocusManager focusManager, @Nonnull Disposable parent) {
    super(project, actionManager, focusManager, parent);
  }

  @Override
  public boolean useSmallLabels() {
    return true;
  }

  public boolean shouldAddToGlobal(Point point) {
    TabLabel label = getSelectedLabel();
    if (label == null || point == null) {
      return true;
    }
    Rectangle bounds = label.getBounds();
    return point.y <= bounds.y + bounds.height;
  }

  @Override
  public void processDropOver(TabInfo over, RelativePoint relativePoint) {
    Point point = relativePoint.getPoint(getComponent());
    myShowDropLocation = shouldAddToGlobal(point);
    super.processDropOver(over, relativePoint);
    for (Map.Entry<TabInfo, TabLabel> entry : myInfo2Label.entrySet()) {
      TabLabel label = entry.getValue();
      if (label.getBounds().contains(point) && myDropInfo != entry.getKey()) {
        select(entry.getKey(), false);
        break;
      }
    }
  }

  @Override
  protected TabLabel createTabLabel(TabInfo info) {
    return new MyTabLabel(this, info);
  }

  private static class MyTabLabel extends TabLabel {
    public MyTabLabel(JBTabsImpl tabs, TabInfo info) {
      super(tabs, info);
    }
  }
}
