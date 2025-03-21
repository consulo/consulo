/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.impl.content;

import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.awt.JBUI;

import java.awt.*;

class ComboContentLayout extends ContentLayout {

  ContentComboLabel myComboLabel;

  ComboContentLayout(DesktopToolWindowContentUi ui) {
    super(ui);
  }

  @Override
  public void init() {
    reset();

    myIdLabel = new BaseLabel(myUi);
    myComboLabel = new ContentComboLabel(this);
  }

  @Override
  public void reset() {
    myIdLabel = null;
    myComboLabel = null;
  }

  @Override
  public void layout() {
    Rectangle bounds = myUi.getBounds();
    Dimension idSize = isIdVisible() ? myIdLabel.getPreferredSize() : JBUI.emptySize();

    int eachX = 0;
    int eachY = 0;

    myIdLabel.setBounds(eachX, eachY, idSize.width, bounds.height);
    eachX += idSize.width;

    Dimension comboSize = myComboLabel.getPreferredSize();
    int spaceLeft = bounds.width - eachX - (isToDrawCombo() && isIdVisible() ? JBUI.scale(3) : 0);

    int width = comboSize.width;
    if (width > spaceLeft) {
      width = spaceLeft;
    }

    myComboLabel.setBounds(eachX, eachY, width, bounds.height);
  }

  @Override
  public int getMinimumWidth() {
    return myIdLabel != null ? myIdLabel.getPreferredSize().width : 0;
  }

  @Override
  public void paintComponent(Graphics g) {
  }

  @Override
  public void paintChildren(Graphics g) {
  }

  @Override
  public void update() {
    updateIdLabel(myIdLabel);
    myComboLabel.update();
  }

  @Override
  public void rebuild() {
    myUi.removeAll();

    myUi.add(myIdLabel);
    DesktopToolWindowContentUi.initMouseListeners(myIdLabel, myUi, true);

    myUi.add(myComboLabel);
    DesktopToolWindowContentUi.initMouseListeners(myComboLabel, myUi, false);
  }

  boolean isToDrawCombo() {
    return myUi.myManager.getContentCount() > 1;
  }

  @Override
  public void contentAdded(ContentManagerEvent event) {
  }

  @Override
  public void contentRemoved(ContentManagerEvent event) {
  }

  @Override
  public void showContentPopup(ListPopup listPopup) {
    final int width = myComboLabel.getSize().width;
    listPopup.setMinimumSize(new Dimension(width, 0));
    listPopup.show(new RelativePoint(myComboLabel, new Point(-2, myComboLabel.getHeight())));
  }

  @Override
  public RelativeRectangle getRectangleFor(Content content) {
    return null;
  }

  @Override
  public Component getComponentFor(Content content) {
    return null;
  }

  @Override
  public String getCloseActionName() {
    return "Close View";
  }

  @Override
  public String getCloseAllButThisActionName() {
    return "Close Other Views";
  }

  @Override
  public String getPreviousContentActionName() {
    return "Select Previous View";
  }

  @Override
  public String getNextContentActionName() {
    return "Select Next View";
  }
}
