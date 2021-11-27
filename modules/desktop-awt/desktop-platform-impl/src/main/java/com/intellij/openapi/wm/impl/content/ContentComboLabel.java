/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.content;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.Gray;
import com.intellij.ui.content.Content;
import com.intellij.ui.popup.PopupState;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ContentComboLabel extends BaseLabel {
  private final PopupState<JBPopup> myPopupState = PopupState.forPopup();

  private final ComboIcon myComboIcon = new ComboIcon() {
    @Override
    public Rectangle getIconRec() {
      return new Rectangle(getWidth() - getIconWidth() - JBUI.scale(3), 0, getIconWidth(), getHeight());
    }

    @Override
    public boolean isActive() {
      return myUi.myWindow.isActive();
    }
  };

  private final ComboContentLayout myLayout;

  public ContentComboLabel(ComboContentLayout layout) {
    super(layout.myUi, true);
    myLayout = layout;
    addMouseListener(new MouseAdapter(){});
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    super.processMouseEvent(e);

    if (UIUtil.isActionClick(e)) {
      if (myPopupState.isRecentlyHidden()) return; // do not show new popup
      DesktopToolWindowContentUi.toggleContentPopup(myUi, myUi.getContentManager(), myPopupState);
    }
  }

  void update() {
    if (isToDrawCombo()) {
      setBorder(JBUI.Borders.empty(0, 8, 0, 8));
    } else {
      setBorder(null);
    }

    updateTextAndIcon(myUi.myManager.getSelectedContent(), true);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    if (!isPreferredSizeSet() && isToDrawCombo()) {
      size.width += myComboIcon.getIconWidth();
    }
    return size;
  }

  private boolean isToDrawCombo() {
    return myLayout.isToDrawCombo();
  }

  @Override
  protected void paintChildren(Graphics g) {
    super.paintChildren(g);
    if (isToDrawCombo()) {
      myComboIcon.paintIcon(this, g);
      g.setColor(Gray._255.withAlpha(100));
    }
  }

  @Override
  public Content getContent() {
    return myUi.myManager.getSelectedContent();
  }
}
