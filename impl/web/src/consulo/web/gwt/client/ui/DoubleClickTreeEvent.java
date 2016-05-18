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

import com.google.gwt.event.shared.GwtEvent;
import org.cafesip.gwtcomp.client.ui.SuperTreeItem;

/**
 * @author VISTALL
 * @since 18-May-16
 */
public class DoubleClickTreeEvent extends GwtEvent<DoubleClickTreeHandler> {

  /**
   * Handler type.
   */
  private static Type<DoubleClickTreeHandler> TYPE;


  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<DoubleClickTreeHandler> getType() {
    if (TYPE == null) {
      TYPE = new Type<DoubleClickTreeHandler>();
    }
    return TYPE;
  }

  private SuperTreeItem myItem;

  public DoubleClickTreeEvent(SuperTreeItem item) {
    myItem = item;
  }

  public SuperTreeItem getItem() {
    return myItem;
  }

  @Override
  public Type<DoubleClickTreeHandler> getAssociatedType() {
    return getType();
  }

  @Override
  protected void dispatch(DoubleClickTreeHandler handler) {
    handler.onDoubleClick(this);
  }
}
