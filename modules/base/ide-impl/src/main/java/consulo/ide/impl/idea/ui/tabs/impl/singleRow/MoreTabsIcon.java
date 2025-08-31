/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.tabs.impl.singleRow;

import consulo.application.AllIcons;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author pegov
 */
public abstract class MoreTabsIcon {
  private final Image icon = AllIcons.Actions.FindAndShowNextMatchesSmall;
  private int myCounter;

  public void paintIcon(Component c, Graphics graphics) {
    if (myCounter <= 0) return;
    Rectangle moreRect = getIconRec();

    if (moreRect == null) return;

    int iconY = getIconY(moreRect);
    int iconX = getIconX(moreRect);

    TargetAWT.to(icon).paintIcon(c, graphics, iconX, iconY);
  }

  public int getIconWidth() {
    return icon.getWidth();
  }

  public int getIconHeight() {
    return icon.getHeight();
  }

  protected int getIconX(Rectangle iconRec) {
    return iconRec.x + iconRec.width / 2 - (getIconWidth()) / 2;
  }

  protected int getIconY(Rectangle iconRec) {
    return iconRec.y + iconRec.height / 2 - getIconHeight() / 2;
  }

  @Nullable
  protected abstract Rectangle getIconRec();

  public void updateCounter(int counter) {
    myCounter = counter;
  }
}
