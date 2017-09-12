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

import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * @author VISTALL
 * @since 14-Jun-16
 * <p>
 * wrapper for menu item
 */
public class GwtMenuItemImpl extends SimplePanel {
  private final MenuItem myItem;

  public GwtMenuItemImpl() {
    myItem = new MenuItem("", () -> {
    });
  }

  public MenuItem getItem() {
    return myItem;
  }
}
