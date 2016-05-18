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
package consulo.web.gwt.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import org.cafesip.gwtcomp.client.ui.SuperTree;
import org.cafesip.gwtcomp.client.ui.SuperTreeItem;

/**
 * @author VISTALL
 * @since 18-May-16
 */
public class DoubleClickTree extends SuperTree {
  private long lastClickTime = System.currentTimeMillis();

  public void addDoubleClickHandler(DoubleClickTreeHandler clickTreeHandler) {
    addHandler(clickTreeHandler, DoubleClickTreeEvent.getType());
  }

  @Override
  public void onBrowserEvent(Event event) {
    int eventType = DOM.eventGetType(event);
    switch (eventType) {
      case Event.ONCLICK:
        long time = System.currentTimeMillis();
        super.onBrowserEvent(event);

        long l = time - lastClickTime;
        lastClickTime = time;

        if (l < 250) {
          SuperTreeItem selectedItem = (SuperTreeItem)getSelectedItem();
          if(selectedItem == null) {
            return;
          }

          fireEvent(new DoubleClickTreeEvent(selectedItem));
        }
        else {
          lastClickTime = time;
        }
        break;
      default:
        super.onBrowserEvent(event);
        break;
    }
  }
}
