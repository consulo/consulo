/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl.layout;

import consulo.ui.ex.action.ActionManager;
import consulo.ide.impl.idea.ui.tabs.impl.JBEditorTabs;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.layout.TabbedLayout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class DesktopTabbedLayoutImpl extends SwingComponentDelegate<JBEditorTabs> implements TabbedLayout {
  class MyJTabbedPane extends JBEditorTabs implements FromSwingComponentWrapper {
    public MyJTabbedPane() {
      super(null, ActionManager.getInstance(), null, null);

      setFirstTabOffset(10);
    }

    @Override
    public boolean isAlphabeticalMode() {
      return false;
    }

    @Override
    public boolean supportsCompression() {
      return false;
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopTabbedLayoutImpl.this;
    }
  }

  public DesktopTabbedLayoutImpl() {
    initialize(new MyJTabbedPane());
  }

  @Nonnull
  @Override
  public Tab createTab() {
    return new DesktopTabImpl(this);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull Tab tab, @Nonnull Component component) {
    DesktopTabImpl desktopTab = (DesktopTabImpl)tab;

    desktopTab.setComponent(component);

    toAWTComponent().addTab(desktopTab.getTabInfo());

    return tab;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull String tabName, @Nonnull Component component) {
    Tab tab = createTab();
    tab.append(tabName);
    return addTab(tab, component);
  }

  @Override
  public void removeTab(@Nonnull Tab tab) {
    DesktopTabImpl desktopTab = (DesktopTabImpl)tab;
    toAWTComponent().removeTab(desktopTab.getTabInfo());
  }
}
