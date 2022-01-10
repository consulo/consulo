/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.vcs.log.ui.frame;

import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.AsyncProcessIcon;
import consulo.ui.image.Image;
import icons.VcsLogIcons;
import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

public class TableWithProgress extends JBTable {
  public TableWithProgress(@Nonnull TableModel model) {
    super(model);
  }

  @Nonnull
  @Override
  protected AsyncProcessIcon createBusyIcon() {
    return new LastRowLoadingIcon();
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    if (isBusy()) {
      return new Dimension(size.width, size.height + myBusyIcon.getPreferredSize().height);
    }
    return size;
  }

  protected boolean isBusy() {
    return myBusyIcon != null && myBusyIcon.isRunning();
  }

  @Override
  protected void paintComponent(@Nonnull Graphics g) {
    super.paintComponent(g);
    if (isBusy()) {
      int preferredHeight = super.getPreferredSize().height;
      paintFooter(g, 0, preferredHeight, getWidth(), getHeight() - preferredHeight);
    }
  }

  protected void paintFooter(@Nonnull Graphics g, int x, int y, int width, int height) {
    g.setColor(getBackground());
    g.fillRect(x, y, width, height);
  }

  private class LastRowLoadingIcon extends AsyncProcessIcon {
    public LastRowLoadingIcon() {
      super(TableWithProgress.this.toString(),
            new Image[]{VcsLogIcons.Process.Dots_2, VcsLogIcons.Process.Dots_3, VcsLogIcons.Process.Dots_4, VcsLogIcons.Process.Dots_5},
            VcsLogIcons.Process.Dots_1);
    }

    @Nonnull
    @Override
    protected Rectangle calculateBounds(@Nonnull JComponent container) {
      Dimension iconSize = getPreferredSize();
      return new Rectangle((container.getWidth() - iconSize.width) / 2, container.getPreferredSize().height - iconSize.height, iconSize.width,
                           iconSize.height);
    }
  }
}
