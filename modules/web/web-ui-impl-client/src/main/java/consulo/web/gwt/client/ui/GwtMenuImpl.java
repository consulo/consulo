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
package consulo.web.gwt.client.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.Log;

import java.util.List;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class GwtMenuImpl extends SimplePanel {
  private MenuItem myMenu;

  public GwtMenuImpl() {
    myMenu = new MenuItem("", (Scheduler.ScheduledCommand)null);
  }

  public MenuItem getMenu() {
    return myMenu;
  }

  public void setChildren(List<Widget> children) {
    MenuBar bar = new MenuBar(true);

    Log.log("creating sub menu. Item " + myMenu.getTitle());

    GwtMenuBarImplConnector.addItems(children, bar);

    myMenu.setSubMenu(bar);
  }
}
