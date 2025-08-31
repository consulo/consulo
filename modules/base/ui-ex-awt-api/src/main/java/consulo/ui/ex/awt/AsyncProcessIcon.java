/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ui.ex.awt;

import consulo.application.AllIcons;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

public class AsyncProcessIcon extends AnimatedIconComponent {
  private static final Image[] SMALL_ICONS = AnimatedIcon.Default.ICONS.toArray(new Image[0]);

  public static final int COUNT = SMALL_ICONS.length;
  public static final int CYCLE_LENGTH = AnimatedIcon.Default.DELAY * SMALL_ICONS.length;

  public AsyncProcessIcon(@NonNls String name) {
    this(name, SMALL_ICONS, AllIcons.Process.Step_passive);
  }

  public AsyncProcessIcon(@NonNls String name, Image[] icons, Image passive) {
    super(name, icons, passive, CYCLE_LENGTH);
  }

  @Override
  protected Dimension calcPreferredSize() {
    return new Dimension(myPassiveIcon.getWidth(), myPassiveIcon.getHeight());
  }

  @Override
  protected void paintIcon(Graphics g, Image icon, int x, int y) {
    super.paintIcon(g, icon, x, y);
  }

  public void updateLocation(JComponent container) {
    Rectangle newBounds = calculateBounds(container);
    if (!newBounds.equals(getBounds())) {
      setBounds(newBounds);
      // painting problems with scrollpane
      // repaint shouldn't be called from paint method
      SwingUtilities.invokeLater(container::repaint);
    }
  }

  @Nonnull
  protected Rectangle calculateBounds(@Nonnull JComponent container) {
    Rectangle rec = container.getVisibleRect();
    Dimension iconSize = getPreferredSize();
    return new Rectangle(rec.x + rec.width - iconSize.width, rec.y, iconSize.width, iconSize.height);
  }

  public static class Big extends AsyncProcessIcon {
    private static final Image[] BIG_ICONS = AnimatedIcon.Big.ICONS.toArray(new Image[0]);

    public Big(@NonNls String name) {
      super(name, BIG_ICONS, AllIcons.Process.Big.Step_passive);
    }
  }

  public boolean isDisposed() {
    return myAnimator.isDisposed();
  }
}
