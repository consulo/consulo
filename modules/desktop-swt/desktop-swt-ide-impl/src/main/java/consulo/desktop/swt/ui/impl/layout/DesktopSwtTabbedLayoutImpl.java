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
package consulo.desktop.swt.ui.impl.layout;

import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TabbedLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtTabbedLayoutImpl extends SWTComponentDelegate<CTabFolder> implements TabbedLayout {
  private final List<DesktopSwtTabImpl> myTabs = new ArrayList<>();

  @Override
  protected CTabFolder createSWT(Composite parent) {
    return new CTabFolder(parent, SWT.TOP | SWT.FLAT);
  }

  @Override
  protected void initialize(CTabFolder component) {
    super.initialize(component);

    for (DesktopSwtTabImpl tab : myTabs) {
      tab.initialize(component);
    }

    component.setSelection(myTabs.size() - 1);
  }

  private void init(DesktopSwtTabImpl tab) {
    CTabFolder tabFolder = toSWTComponent();

    if (tabFolder != null) {
      tab.initialize(tabFolder);

      tabFolder.setSelection(tab.getTabItem());
    }
  }

  @Nonnull
  @Override
  public Tab createTab() {
    return new DesktopSwtTabImpl();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull Tab tab, @Nonnull Component component) {
    DesktopSwtTabImpl swtTab = (DesktopSwtTabImpl)tab;

    myTabs.add(swtTab);
    swtTab.setComponent(component);

    init(swtTab);
    return tab;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull String tabName, @Nonnull Component component) {
    DesktopSwtTabImpl tab = new DesktopSwtTabImpl();
    myTabs.add(tab);
    tab.append(tabName);
    tab.setComponent(component);

    init(tab);
    return tab;
  }

  @Override
  public void removeTab(@Nonnull Tab tab) {

  }
}
