// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class RelativePoint {
  
  private final Component myComponent;
  
  private final Point myPointOnComponent;

  
  private final Component myOriginalComponent;
  
  private final Point myOriginalPoint;

  public RelativePoint(MouseEvent event) {
    this(event.getComponent(), event.getPoint());
  }

  public RelativePoint(Point screenPoint) {
    this(getTargetWindow(), calcPoint(screenPoint));
  }

  
  private static Point calcPoint(Point screenPoint) {
    Point p = new Point(screenPoint.x, screenPoint.y);
    SwingUtilities.convertPointFromScreen(p, getTargetWindow());
    return p;
  }

  
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

  public RelativePoint(Component aComponent, Point aPointOnComponent) {
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

  
  public RelativePoint getPointOn(Component aTargetComponent) {
    Point point = getPoint(aTargetComponent);
    return new RelativePoint(aTargetComponent, point);
  }

  
  public Point getScreenPoint() {
    Point point = (Point)getPoint().clone();
    SwingUtilities.convertPointToScreen(point, getComponent());
    return point;
  }

  
  public MouseEvent toMouseEvent() {
    return new MouseEvent(myComponent, 0, 0, 0, myPointOnComponent.x, myPointOnComponent.y, 1, false);
  }

  @Override
  
  public String toString() {
    //noinspection HardCodedStringLiteral
    return getPoint() + " on " + getComponent();
  }

  
  public static RelativePoint getCenterOf(JComponent component) {
    Rectangle visibleRect = component.getVisibleRect();
    Point point = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
    return new RelativePoint(component, point);
  }

  
  public static RelativePoint getSouthEastOf(JComponent component) {
    Rectangle visibleRect = component.getVisibleRect();
    Point point = new Point(visibleRect.x + visibleRect.width, visibleRect.y + visibleRect.height);
    return new RelativePoint(component, point);
  }

  
  public static RelativePoint getSouthWestOf(JComponent component) {
    Rectangle visibleRect = component.getVisibleRect();
    Point point = new Point(visibleRect.x, visibleRect.y + visibleRect.height);
    return new RelativePoint(component, point);
  }

  
  public static RelativePoint getSouthOf(JComponent component) {
    Rectangle visibleRect = component.getVisibleRect();
    Point point = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height);
    return new RelativePoint(component, point);
  }

  
  public static RelativePoint getNorthWestOf(JComponent component) {
    Rectangle visibleRect = component.getVisibleRect();
    Point point = new Point(visibleRect.x, visibleRect.y);
    return new RelativePoint(component, point);
  }

  
  @SuppressWarnings("unused")
  public static RelativePoint getNorthEastOf(JComponent component) {
    Rectangle visibleRect = component.getVisibleRect();
    Point point = new Point(visibleRect.x + visibleRect.width, visibleRect.y);
    return new RelativePoint(component, point);
  }

  
  public static RelativePoint fromScreen(Point screenPoint) {
    Frame root = JOptionPane.getRootFrame();
    SwingUtilities.convertPointFromScreen(screenPoint, root);
    return new RelativePoint(root, screenPoint);
  }

  
  public Component getOriginalComponent() {
    return myOriginalComponent;
  }

  
  public Point getOriginalPoint() {
    return myOriginalPoint;
  }
}
