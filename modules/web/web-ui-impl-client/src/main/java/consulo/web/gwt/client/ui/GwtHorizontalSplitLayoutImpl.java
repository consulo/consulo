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
package consulo.web.gwt.client.ui;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author VISTALL
 * @since 13-Sep-17
 * <p>
 * idea from https://stackoverflow.com/a/16403608/3129079
 */
public class GwtHorizontalSplitLayoutImpl extends SplitLayoutPanel {
  private ScrollPanel myLeftScrollPanel = new ScrollPanel();
  private ScrollPanel myRightScrollPanel = new ScrollPanel();

  private int myProportion = 50;
  private int myLastWidth;

  public GwtHorizontalSplitLayoutImpl() {
    this(1);
  }

  public GwtHorizontalSplitLayoutImpl(int splitterSize) {
    super(splitterSize);
    setStyleName("ui-horizontal-split-panel");
    addWest(myLeftScrollPanel, 300);
    add(myRightScrollPanel);
  }

  public void setLeftWidget(Widget widget) {
    myLeftScrollPanel.clear();
    myLeftScrollPanel.add(widget);
    setWidgetToggleDisplayAllowed(myLeftScrollPanel, true);
  }

  public void setRightWidget(Widget widget) {
    myRightScrollPanel.clear();
    myRightScrollPanel.add(widget);
  }

  public void setSplitPosition(String percent) {
    percent = percent.replace("%", "");
    int p = Integer.parseInt(percent);
    myProportion = p;

    if (!isAttached()) {
      return;
    }

    int offsetWidth = getOffsetWidth();
    if (offsetWidth == 0) {
      offsetWidth = myLastWidth;
    }
    else {
      myLastWidth = offsetWidth;
    }

    double size = (offsetWidth * p) / 100.0;
    setWidgetSize(myLeftScrollPanel, size);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    setSplitPosition(myProportion + "%");
  }

  @Override
  public void onResize() {
    super.onResize();

    int offsetWidth = getOffsetWidth();
    if (offsetWidth == 0) {
      offsetWidth = myLastWidth;
    }
    else {
      myLastWidth = offsetWidth;
    }

    double elementSize = myLeftScrollPanel.getOffsetWidth();

    myProportion = (int)(elementSize / offsetWidth * 100.);
  }

  public void updateOnResize() {
    setSplitPosition(myProportion + "%");
  }
}
