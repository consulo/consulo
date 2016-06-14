/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwtUI.client.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwtUI.client.UIConverter;
import consulo.web.gwtUI.client.WebSocketProxy;
import consulo.web.gwtUI.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class GwtMenuImpl extends MenuItem implements InternalGwtComponent, InternalGwtComponentWithChildren {

  public GwtMenuImpl() {
    super("", (Scheduler.ScheduledCommand)null);
  }

  @Override
  public void updateState(@NotNull Map<String, String> map) {
    setText(map.get("text"));
  }

  @Override
  public Widget asWidget() {
    return null;
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    MenuBar bar = new MenuBar(true);
    for (UIComponent.Child child : children) {
      final InternalGwtComponent component = UIConverter.create(proxy, child.getComponent());

      if(component instanceof GwtMenuSeparatorImpl) {
        bar.addSeparator();
      }
      else {
        bar.addItem((MenuItem)component);
      }
    }
    setSubMenu(bar);
  }
}
