/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tab;

import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.awt.util.TimedDeadzone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

public interface JBTabsPresentation {

  boolean isHideTabs();

  void setHideTabs(boolean hideTabs);

  @Deprecated
  @DeprecationInfo("Doing nothing")
  default JBTabsPresentation setPaintBorder(int top, int left, int right, int bottom) {
    return this;
  }

  @Deprecated
  @DeprecationInfo("Doing nothing")
  default JBTabsPresentation setTabSidePaintBorder(int size) {
    return this;
  }

  JBTabsPresentation setPaintFocus(boolean paintFocus);

  @Deprecated
  @DeprecationInfo("Doing nothing")
  default JBTabsPresentation setAlwaysPaintSelectedTab(boolean paintSelected) {
    return this;
  }

  JBTabsPresentation setStealthTabMode(boolean stealthTabMode);

  JBTabsPresentation setSideComponentVertical(boolean vertical);

  JBTabsPresentation setSideComponentOnTabs(boolean onTabs);

  JBTabsPresentation setSideComponentBefore(boolean before);

  JBTabsPresentation setSingleRow(boolean singleRow);

  boolean isSingleRow();

  JBTabsPresentation setUiDecorator(@Nullable UiDecorator decorator);

  JBTabsPresentation setRequestFocusOnLastFocusedComponent(boolean request);

  void setPaintBlocked(boolean blocked, boolean takeSnapshot);

  JBTabsPresentation setInnerInsets(Insets innerInsets);

  @Deprecated
  @DeprecationInfo("Doing nothing")
  default JBTabsPresentation setGhostsAlwaysVisible(boolean visible) {
    return this;
  }

  JBTabsPresentation setFocusCycle(boolean root);

  @Nonnull
  JBTabsPresentation setToDrawBorderIfTabsHidden(boolean draw);

  @Nonnull
  JBTabs getJBTabs();

  @Nonnull
  JBTabsPresentation setActiveTabFillIn(@Nullable Color color);

  @Nonnull
  JBTabsPresentation setTabLabelActionsMouseDeadzone(TimedDeadzone.Length length);

  @Nonnull
  JBTabsPresentation setTabsPosition(JBTabsPosition position);

  JBTabsPosition getTabsPosition();

  JBTabsPresentation setTabDraggingEnabled(boolean enabled);
}
