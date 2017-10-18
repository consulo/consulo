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
import consulo.web.gwt.client.util.GwtUIUtil;

/**
 * @author VISTALL
 * @since 13-Oct-17
 */
class GwtToolWindowStripeInner extends SimplePanel {
  private boolean myVertical;

  private ComplexPanel myPrimary;
  private ComplexPanel mySecondary;

  public GwtToolWindowStripeInner(boolean vertical) {
    myVertical = vertical;
    GwtUIUtil.fill(this);

    myPrimary = createInner();
    mySecondary = createInner();

    CellPanel inner = createInner();
    GwtUIUtil.fill(inner);
    setWidget(inner);

    inner.add(myPrimary);
    FlowPanel filler = GwtUIUtil.fillAndReturn(new FlowPanel());
    inner.add(filler);
    inner.add(mySecondary);
    if (vertical) {
      inner.setCellHeight(filler, "100%");
    }
    else {
      inner.setCellWidth(filler, "100%");
    }
  }

  public void add(Widget widget, boolean secondary) {
    if (secondary) {
      mySecondary.add(widget);
    }
    else {
      myPrimary.add(widget);
    }
  }

  public void removeAll() {
    if (myPrimary != null) {
      myPrimary.clear();
    }
    if (mySecondary != null) {
      mySecondary.clear();
    }
  }

  private CellPanel createInner() {
    if (myVertical) {
      VerticalPanel panel = new VerticalPanel();
      panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      return panel;
    }
    else {
      return new HorizontalPanel();
    }
  }
}
