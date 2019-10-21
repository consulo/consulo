// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.awt;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class RelativePoint {
  @Nonnull
  private final Component myComponent;
  @Nonnull
  private final Point myPointOnComponent;

  @Nonnull
  private final Component myOriginalComponent;
  @Nonnull
  private final Point myOriginalPoint;

  public RelativePoint(@Nonnull MouseEvent event) {
    this(event.getComponent(), event.getPoint());
  }

  public RelativePoint(@Nonnull Point screenPoint) {
    this(getTargetWindow(), calcPoint(screenPoint));
  }

  @Nonnull
  private static Point calcPoint(@Nonnull Point screenPoint) {
    Point p = new Point(screenPoint.x, screenPoint.y);
    SwingUtilities.convertPointFromScreen(p, getTargetWindow());
    return p;
  }

  @Nonnull
  private static Window getTargetWindow() {
    Window[] windows = Window.getWindows();
    Window targetWindow = null;
    for (Window each : windows) {
      if (each.isActive()) {
        targetWindow = each;
        break;
      }
    }

    if (targetWindow == null) {
      targetWindow = JOptionPane.getRootFrame();
    }
    return targetWindow;
  }

  public RelativePoint(@Nonnull Component aComponent, @Nonnull Point aPointOnComponent) {
    JRootPane rootPane = SwingUtilities.getRootPane(aComponent);
    if (aComponent.isShowing() && rootPane != null) {
      myComponent = rootPane;
      myPointOnComponent = SwingUtilities.convertPoint(aComponent, aPointOnComponent, myComponent);
    }
    else {
      myComponent = aComponent;
      myPointOnComponent = aPointOnComponent;
    }
    myOriginalComponent = aComponent;
    myOriginalPoint = aPointOnComponent;
  }

  @Nonnull
  public Component getComponent() {
    return myComponent;
  }

  public Point getPoint() {
    return myPointOnComponent;
  }

  public Point getPoint(@Nullable Component aTargetComponent) {
    //todo: remove that after implementation of DND to html design time controls
    boolean window = aTargetComponent instanceof Window;
    if (aTargetComponent == null || !window && (aTargetComponent.getParent() == null || SwingUtilities.getWindowAncestor(aTargetComponent) == null)) {
      return new Point();
    }

    return SwingUtilities.convertPoint(getComponent(), getPoint(), aTargetComponent);
  }

  @Nonnull
  public RelativePoint getPointOn(@Nonnull Component aTargetComponent) {
    final Point point = getPoint(aTargetComponent);
    return new RelativePoint(aTargetComponent, point);
  }

  @Nonnull
  public Point getScreenPoint() {
    final Point point = (Point)getPoint().clone();
    SwingUtilities.convertPointToScreen(point, getComponent());
    return point;
  }

  @Nonnull
  public MouseEvent toMouseEvent() {
    return new MouseEvent(myComponent, 0, 0, 0, myPointOnComponent.x, myPointOnComponent.y, 1, false);
  }

  @Override
  @Nonnull
  public String toString() {
    //noinspection HardCodedStringLiteral
    return getPoint() + " on " + getComponent();
  }

  @Nonnull
  public static RelativePoint getCenterOf(@Nonnull JComponent component) {
    final Rectangle visibleRect = component.getVisibleRect();
    final Point point = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
    return new RelativePoint(component, point);
  }

  @Nonnull
  public static RelativePoint getSouthEastOf(@Nonnull JComponent component) {
    final Rectangle visibleRect = component.getVisibleRect();
    final Point point = new Point(visibleRect.x + visibleRect.width, visibleRect.y + visibleRect.height);
    return new RelativePoint(component, point);
  }

  @Nonnull
  public static RelativePoint getSouthWestOf(@Nonnull JComponent component) {
    final Rectangle visibleRect = component.getVisibleRect();
    final Point point = new Point(visibleRect.x, visibleRect.y + visibleRect.height);
    return new RelativePoint(component, point);
  }

  @Nonnull
  public static RelativePoint getSouthOf(@Nonnull JComponent component) {
    final Rectangle visibleRect = component.getVisibleRect();
    final Point point = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height);
    return new RelativePoint(component, point);
  }

  @Nonnull
  public static RelativePoint getNorthWestOf(@Nonnull JComponent component) {
    final Rectangle visibleRect = component.getVisibleRect();
    final Point point = new Point(visibleRect.x, visibleRect.y);
    return new RelativePoint(component, point);
  }

  @Nonnull
  @SuppressWarnings("unused")
  public static RelativePoint getNorthEastOf(@Nonnull JComponent component) {
    final Rectangle visibleRect = component.getVisibleRect();
    final Point point = new Point(visibleRect.x + visibleRect.width, visibleRect.y);
    return new RelativePoint(component, point);
  }

  @Nonnull
  public static RelativePoint fromScreen(Point screenPoint) {
    Frame root = JOptionPane.getRootFrame();
    SwingUtilities.convertPointFromScreen(screenPoint, root);
    return new RelativePoint(root, screenPoint);
  }

  @Nonnull
  public Component getOriginalComponent() {
    return myOriginalComponent;
  }

  @Nonnull
  public Point getOriginalPoint() {
    return myOriginalPoint;
  }
}
