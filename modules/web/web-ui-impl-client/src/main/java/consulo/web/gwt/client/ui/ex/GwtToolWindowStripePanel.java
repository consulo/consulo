/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.client.ui.ex;

import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class GwtToolWindowStripePanel implements IsWidget {
  private final DockLayoutState.Constraint myPosition;
  private final CellPanel myCellPanel;

  public GwtToolWindowStripePanel(DockLayoutState.Constraint position) {
    myPosition = position;
    switch (position) {
      case TOP:
      case BOTTOM:
        HorizontalPanel horizontalPanel = new HorizontalPanel();
        myCellPanel = horizontalPanel;
        break;
      case LEFT:
      case RIGHT:
        VerticalPanel verticalPanel = new VerticalPanel();
        myCellPanel = verticalPanel;
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }


  @Override
  public Widget asWidget() {
    return myCellPanel;
  }
}
