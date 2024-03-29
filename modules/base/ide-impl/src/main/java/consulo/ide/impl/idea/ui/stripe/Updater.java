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
package consulo.ide.impl.idea.ui.stripe;

import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.disposer.Disposable;
import consulo.ui.ex.awt.ScrollBarUIConstants;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Sergey.Malenkov
 */
public abstract class Updater<Painter extends ErrorStripePainter> implements Disposable {
  private final Painter myPainter;
  private final JScrollBar myScrollBar;
  private final MergingUpdateQueue myQueue;
  private final MouseAdapter myMouseAdapter = new MouseAdapter() {
    @Override
    public void mouseMoved(MouseEvent event) {
      onMouseMove(myPainter, event.getX(), event.getY());
    }

    @Override
    public void mouseClicked(MouseEvent event) {
      onMouseClick(myPainter, event.getX(), event.getY());
    }
  };

  protected Updater(@Nonnull Painter painter, JScrollPane pane) {
    this(painter, pane.getVerticalScrollBar());
  }

  protected Updater(@Nonnull Painter painter, JScrollBar bar) {
    myPainter = painter;
    myScrollBar = bar;
    myScrollBar.addMouseListener(myMouseAdapter);
    myScrollBar.addMouseMotionListener(myMouseAdapter);
    myQueue = new MergingUpdateQueue("ErrorStripeUpdater", 100, true, myScrollBar, this);
    UIUtil.putClientProperty(myScrollBar, ScrollBarUIConstants.TRACK, (g, x, y, width, height, object) -> {
      DaemonCodeAnalyzerSettings settings = DaemonCodeAnalyzerSettings.getInstance();
      myPainter.setMinimalThickness(settings == null ? 2 : Math.min(settings.ERROR_STRIPE_MARK_MIN_HEIGHT, JBUI.scale(4)));
      myPainter.setErrorStripeGap(1);
      if (myPainter instanceof ExtraErrorStripePainter) {
        ExtraErrorStripePainter extra = (ExtraErrorStripePainter)myPainter;
        extra.setGroupSwap(!myScrollBar.getComponentOrientation().isLeftToRight());
      }
      myPainter.paint(g, x, y, width, height, object);
    });
  }

  @Override
  public void dispose() {
    myScrollBar.removeMouseListener(myMouseAdapter);
    myScrollBar.removeMouseMotionListener(myMouseAdapter);
    UIUtil.putClientProperty(myScrollBar, ScrollBarUIConstants.TRACK, null);
  }

  private int findErrorStripeIndex(Painter painter, int x, int y) {
    int index = painter.findIndex(x, y);
    if (null != painter.getErrorStripe(index)) return index;
    index = painter.findIndex(x, y + 1);
    if (null != painter.getErrorStripe(index)) return index;
    index = painter.findIndex(x, y - 1);
    if (null != painter.getErrorStripe(index)) return index;
    index = painter.findIndex(x, y + 2);
    if (null != painter.getErrorStripe(index)) return index;
    return -1;
  }

  protected void onMouseMove(Painter painter, int x, int y) {
    onMouseMove(painter, findErrorStripeIndex(painter, x, y));
  }

  protected void onMouseMove(Painter painter, int index) {
    myScrollBar.setCursor(index < 0 ? null : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  protected void onMouseClick(Painter painter, int x, int y) {
    onMouseClick(painter, findErrorStripeIndex(painter, x, y));
  }

  protected void onMouseClick(Painter painter, int index) {
    onSelect(painter, index);
  }

  protected void onSelect(Painter painter, int index) {
  }

  protected ShortcutSet getNextErrorShortcut() {
    return new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts("GotoNextError"));
  }

  public void selectNext(int index) {
    onSelect(myPainter, findNextIndex(index));
  }

  protected ShortcutSet getPreviousErrorShortcut() {
    return new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts("GotoPreviousError"));
  }

  public void selectPrevious(int index) {
    onSelect(myPainter, findPreviousIndex(index));
  }

  protected abstract void update(Painter painter);

  protected void update(Painter painter, int index, Object object) {
    painter.setErrorStripe(index, getErrorStripe(object));
  }

  protected ErrorStripe getErrorStripe(Object object) {
    return null;
  }

  public final void update() {
    myQueue.cancelAllUpdates();
    myQueue.queue(new Update("update") {
      @Override
      public void run() {
        update(myPainter);
        if (myPainter.isModified()) {
          myScrollBar.invalidate();
          myScrollBar.repaint();
        }
      }
    });
  }

  public int findNextIndex(int current) {
    int count = myPainter.getErrorStripeCount();
    int foundIndex = -1;
    int foundLayer = 0;
    if (0 <= current && current < count) {
      current++;
      for (int index = current; index < count; index++) {
        int layer = getLayer(index);
        if (layer > foundLayer) {
          foundIndex = index;
          foundLayer = layer;
        }
      }
      for (int index = 0; index < current; index++) {
        int layer = getLayer(index);
        if (layer > foundLayer) {
          foundIndex = index;
          foundLayer = layer;
        }
      }
    }
    else {
      for (int index = 0; index < count; index++) {
        int layer = getLayer(index);
        if (layer > foundLayer) {
          foundIndex = index;
          foundLayer = layer;
        }
      }
    }
    return foundIndex;
  }

  public int findPreviousIndex(int current) {
    int count = myPainter.getErrorStripeCount();
    int foundIndex = -1;
    int foundLayer = 0;
    if (0 <= current && current < count) {
      current--;
      for (int index = count - 1; index >= 0; index++) {
        int layer = getLayer(index);
        if (layer > foundLayer) {
          foundIndex = index;
          foundLayer = layer;
        }
      }
      for (int index = current - 1; index >= 0; index++) {
        int layer = getLayer(index);
        if (layer > foundLayer) {
          foundIndex = index;
          foundLayer = layer;
        }
      }
    }
    else {
      for (int index = count - 1; index >= 0; index--) {
        int layer = getLayer(index);
        if (layer > foundLayer) {
          foundIndex = index;
          foundLayer = layer;
        }
      }
    }
    return foundIndex;
  }

  private int getLayer(int index) {
    ErrorStripe stripe = myPainter.getErrorStripe(index);
    return stripe == null ? -1 : stripe.getLayer();
  }
}
