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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.ui.Painter;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.Set;

final class PaintersHelper implements Painter.Listener {
  private static final Logger LOG = Logger.getInstance(PaintersHelper.class);

  private final Set<Painter> myPainters = ContainerUtil.newLinkedHashSet();
  private final Map<Painter, Component> myPainter2Component = ContainerUtil.newLinkedHashMap();

  private final JComponent myRootComponent;

  public PaintersHelper(@Nonnull JComponent component) {
    myRootComponent = component;
  }

  public boolean hasPainters() {
    return !myPainters.isEmpty();
  }

  public boolean needsRepaint() {
    for (Painter painter : myPainters) {
      if (painter.needsRepaint()) return true;
    }
    return false;
  }

  public void addPainter(@Nonnull Painter painter, @Nullable Component component) {
    myPainters.add(painter);
    myPainter2Component.put(painter, component == null ? myRootComponent : component);
    painter.addListener(this);
  }

  public void removePainter(@Nonnull Painter painter) {
    painter.removeListener(this);
    myPainters.remove(painter);
    myPainter2Component.remove(painter);
  }

  public void clear() {
    for (Painter painter : myPainters) {
      painter.removeListener(this);
    }
    myPainters.clear();
    myPainter2Component.clear();
  }

  public void paint(Graphics g) {
    runAllPainters(g, computeOffsets(g, myRootComponent));
  }

  void runAllPainters(Graphics gg, int[] offsets) {
    if (myPainters.isEmpty()) return;
    Graphics2D g = (Graphics2D)gg;
    AffineTransform orig = g.getTransform();
    int i = 0;
    // restore transform at the time of computeOffset()
    AffineTransform t = new AffineTransform();
    t.translate(offsets[i++], offsets[i++]);

    for (Painter painter : myPainters) {
      if (!painter.needsRepaint()) continue;
      Component cur = myPainter2Component.get(painter);

      g.setTransform(t);
      g.translate(offsets[i++], offsets[i++]);
      // paint in the orig graphics scale (note, the offsets are pre-scaled)
      g.scale(orig.getScaleX(), orig.getScaleY());
      painter.paint(cur, g);
    }
    g.setTransform(orig);
  }

  @Nonnull
  int[] computeOffsets(Graphics gg, @Nonnull JComponent component) {
    if (myPainters.isEmpty()) return ArrayUtil.EMPTY_INT_ARRAY;
    int i = 0;
    int[] offsets = new int[2 + myPainters.size() * 2];
    // store current graphics transform
    Graphics2D g = (Graphics2D)gg;
    AffineTransform tx = g.getTransform();
    // graphics tx offsets include graphics scale
    offsets[i++] = (int)tx.getTranslateX();
    offsets[i++] = (int)tx.getTranslateY();
    // calculate relative offsets for painters
    Rectangle r = null;
    Component prev = null;
    for (Painter painter : myPainters) {
      if (!painter.needsRepaint()) continue;

      Component cur = myPainter2Component.get(painter);
      if (cur != prev || r == null) {
        Container curParent = cur.getParent();
        if (curParent == null) continue;
        r = SwingUtilities.convertRectangle(curParent, cur.getBounds(), component);
        prev = cur;
      }
      // component offsets don't include graphics scale, so compensate
      offsets[i++] = (int)(r.x * tx.getScaleX());
      offsets[i++] = (int)(r.y * tx.getScaleY());
    }
    return offsets;
  }

  @Override
  public void onNeedsRepaint(Painter painter, JComponent dirtyComponent) {
    if (dirtyComponent != null && dirtyComponent.isShowing()) {
      Rectangle rec = SwingUtilities.convertRectangle(dirtyComponent, dirtyComponent.getBounds(), myRootComponent);
      myRootComponent.repaint(rec);
    }
    else {
      myRootComponent.repaint();
    }
  }
}
