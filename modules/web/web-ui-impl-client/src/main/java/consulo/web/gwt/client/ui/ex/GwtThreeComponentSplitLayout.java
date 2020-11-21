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

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.GwtUIUtil;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23-Oct-17
 */
public class GwtThreeComponentSplitLayout extends SimplePanel {
  private SplitLayoutPanel mySplitLayoutPanel;

  private SimplePanel myLeftPanel = new SimplePanel();
  private SimplePanel myRightPanel = new SimplePanel();
  private SimplePanel myCenterPanel = new SimplePanel();

  public GwtThreeComponentSplitLayout() {
    mySplitLayoutPanel = new SplitLayoutPanel(1);

    GwtUIUtil.fill(mySplitLayoutPanel);

    setWidget(mySplitLayoutPanel);

    mySplitLayoutPanel.addWest(myLeftPanel, 250);
    mySplitLayoutPanel.addEast(myRightPanel, 250);
    mySplitLayoutPanel.add(myCenterPanel);

    mySplitLayoutPanel.setWidgetHidden(myLeftPanel, true);
    mySplitLayoutPanel.setWidgetHidden(myRightPanel, true);
    mySplitLayoutPanel.setWidgetHidden(myCenterPanel, true);
  }

  public void rebuild(@Nullable Widget leftWidget, @Nullable Widget rightWidget, @Nullable Widget centerWidget) {
    myLeftPanel.setWidget(leftWidget);
    myRightPanel.setWidget(rightWidget);
    myCenterPanel.setWidget(centerWidget);

    mySplitLayoutPanel.setWidgetHidden(myLeftPanel, leftWidget == null);
    mySplitLayoutPanel.setWidgetHidden(myRightPanel, rightWidget == null);
    mySplitLayoutPanel.setWidgetHidden(myCenterPanel, centerWidget == null);
  }
}
