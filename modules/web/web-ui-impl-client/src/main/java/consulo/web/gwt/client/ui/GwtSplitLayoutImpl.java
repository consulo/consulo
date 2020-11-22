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

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.ApplicationHolder;
import consulo.web.gwt.client.ComponentColors;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.shared.ui.state.RGBColorShared;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 13-Sep-17
 * <p>
 * idea from https://stackoverflow.com/a/16403608/3129079
 */
public abstract class GwtSplitLayoutImpl extends SplitLayoutPanel {
  private SimplePanel myFirstWidget = new SimplePanel();
  private SimplePanel mySecondWidget = new SimplePanel();

  private int myProportion = 50;
  private int myLastElementSize;

  public GwtSplitLayoutImpl() {
    this(1);
  }

  protected abstract int getElementSize(Widget widget);

  protected abstract Direction getDirection();

  public GwtSplitLayoutImpl(int splitterSize) {
    super(splitterSize);

    myFirstWidget.setSize("100%", "100%");
    mySecondWidget.setSize("100%", "100%");

    insert(myFirstWidget, getDirection(), 300, null);

    add(mySecondWidget);
  }

  public void setFirstWidget(@Nullable Widget widget) {
    if (widget != null) {
      widget.setSize("100%", "100%");
    }
    myFirstWidget.clear();
    if (widget != null) {
      myFirstWidget.add(widget);
    }
    setWidgetToggleDisplayAllowed(myFirstWidget, widget != null);
  }

  public void setSecondWidget(@javax.annotation.Nullable Widget widget) {
    if (widget != null) {
      widget.setSize("100%", "100%");
    }
    mySecondWidget.clear();
    if (widget != null) {
      mySecondWidget.add(widget);
    }
  }

  public void setSplitPosition(String percent) {
    percent = percent.replace("%", "");
    int p = Integer.parseInt(percent);
    myProportion = p;

    if (!isAttached()) {
      return;
    }

    int elementHeight = getElementSize(this);
    if (elementHeight == 0) {
      elementHeight = myLastElementSize;
    }
    else {
      myLastElementSize = elementHeight;
    }

    double size = (elementHeight * p) / 100.0;
    setWidgetSize(myFirstWidget, size);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    setSplitPosition(myProportion + "%");
  }

  @Override
  public void onResize() {
    super.onResize();

    int thisElementSize = getElementSize(this);
    if (thisElementSize == 0) {
      thisElementSize = myLastElementSize;
    }
    else {
      myLastElementSize = thisElementSize;
    }

    double elementSize = getElementSize(myFirstWidget);

    myProportion = (int)(elementSize / thisElementSize * 100.);
  }

  @Override
  protected void insertSplitter(Widget widget, Widget before) {
    assert getChildren().size() > 0 : "Can't add a splitter before any children";

    LayoutData layout = (LayoutData)widget.getLayoutData();
    Splitter splitter = null;
    switch (getResolvedDirection(layout.direction)) {
      case WEST:
        splitter = new HSplitter(widget, false);
        break;
      case EAST:
        splitter = new HSplitter(widget, true);
        break;
      case NORTH:
        splitter = new VSplitter(widget, false);
        break;
      case SOUTH:
        splitter = new VSplitter(widget, true);
        break;
      default:
        assert false : "Unexpected direction";
    }

    RGBColorShared borderColor = ApplicationHolder.INSTANCE.getComponentColor(ComponentColors.BORDER);
    splitter.getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(borderColor));

    superInsert(splitter, layout.direction, splitterSize, before);
  }

  public void updateOnResize() {
    setSplitPosition(myProportion + "%");
  }
}