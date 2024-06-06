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
package consulo.ide.impl.idea.ui;

import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.MovablePopup;
import consulo.ide.impl.idea.util.ui.MouseEventHandler;
import consulo.platform.Platform;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.MouseEventAdapter;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractExpandableItemsHandler<KeyType, ComponentType extends JComponent> implements ExpandableItemsHandler<KeyType> {
  protected final ComponentType myComponent;

  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final CellRendererPane myRendererPane = new CellRendererPane();
  private final JComponent myTipComponent = new JComponent() {
    @Override
    protected void paintComponent(Graphics g) {
      Insets insets = getInsets();
      UIUtil.drawImage(g, myImage, insets.left, insets.top, null);
    }
  };

  private boolean myEnabled = Registry.is("ide.expansion.hints.enabled");
  private final MovablePopup myPopup;
  private KeyType myKey;
  private Rectangle myKeyItemBounds;
  private BufferedImage myImage;

  public static void setRelativeBounds(@Nonnull Component parent, @Nonnull Rectangle bounds,
                                       @Nonnull Component child, @Nonnull Container validationParent) {
    validationParent.add(parent);
    parent.setBounds(bounds);
    parent.validate();
    child.setLocation(SwingUtilities.convertPoint(child, 0, 0, parent));
    validationParent.remove(parent);
  }

  protected AbstractExpandableItemsHandler(@Nonnull final ComponentType component) {
    myComponent = component;
    myComponent.add(myRendererPane);
    myComponent.validate();
    myPopup = new MovablePopup(myComponent, myTipComponent);

    MouseEventHandler dispatcher = new MouseEventHandler() {
      @Override
      protected void handle(MouseEvent event) {
        myComponent.dispatchEvent(MouseEventAdapter.convert(event, myComponent));
      }

      @Override
      public void mouseEntered(MouseEvent event) {
      }

      @Override
      public void mouseExited(MouseEvent event) {
        // don't hide the hint if mouse exited to owner component
        if (myComponent.getMousePosition() == null) {
          hideHint();
        }
      }
    };
    myTipComponent.addMouseListener(dispatcher);
    myTipComponent.addMouseMotionListener(dispatcher);
    myTipComponent.addMouseWheelListener(dispatcher);

    MouseEventHandler handler = new MouseEventHandler() {
      @Override
      protected void handle(MouseEvent event) {
        handleMouseEvent(event, MouseEvent.MOUSE_MOVED != event.getID());
      }

      @Override
      public void mouseClicked(MouseEvent event) {
      }

      @Override
      public void mouseExited(MouseEvent event) {
        // don't hide the hint if mouse exited to it
        if (myTipComponent.getMousePosition() == null) {
          hideHint();
        }
      }
    };
    myComponent.addMouseListener(handler);
    myComponent.addMouseMotionListener(handler);

    myComponent.addFocusListener(
            new FocusAdapter() {
              @Override
              public void focusLost(FocusEvent e) {
                onFocusLost();
              }

              @Override
              public void focusGained(FocusEvent e) {
                updateCurrentSelection();
              }
            }
    );

    myComponent.addComponentListener(
            new ComponentAdapter() {
              @Override
              public void componentHidden(ComponentEvent e) {
                hideHint();
              }

              @Override
              public void componentMoved(ComponentEvent e) {
                updateCurrentSelection();
              }

              @Override
              public void componentResized(ComponentEvent e) {
                updateCurrentSelection();
              }
            }
    );

    myComponent.addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
      @Override
      public void ancestorMoved(HierarchyEvent e) {
        updateCurrentSelection();
      }

      @Override
      public void ancestorResized(HierarchyEvent e) {
        updateCurrentSelection();
      }
    });

    myComponent.addHierarchyListener(
            new HierarchyListener() {
              @Override
              public void hierarchyChanged(HierarchyEvent e) {
                hideHint();
              }
            }
    );
  }

  protected void onFocusLost() {
    hideHint();
  }

  @Override
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
    if (!myEnabled) hideHint();
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Nonnull
  @Override
  public Collection<KeyType> getExpandedItems() {
    return myKey == null ? Collections.<KeyType>emptyList() : Collections.singleton(myKey);
  }

  protected void updateCurrentSelection() {
    handleSelectionChange(myKey, true);
  }

  protected void handleMouseEvent(MouseEvent e, boolean forceUpdate) {
    KeyType selected = getCellKeyForPoint(e.getPoint());
    if (forceUpdate || !Comparing.equal(myKey, selected)) {
      handleSelectionChange(selected, true);
    }

    // Temporary workaround
    if (e.getClickCount() == 2) {
      hideHint();
    }
  }

  protected void handleSelectionChange(KeyType selected) {
    handleSelectionChange(selected, false);
  }

  protected void handleSelectionChange(final KeyType selected, final boolean processIfUnfocused) {
    if (!EventQueue.isDispatchThread()) {
      return;
    }
    myUpdateAlarm.cancelAllRequests();
    if (selected == null || !isHandleSelectionEnabled(selected, processIfUnfocused)) {
      hideHint();
      return;
    }
    if (!selected.equals(myKey)) {
      hideHint();
    }
    myUpdateAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        doHandleSelectionChange(selected, processIfUnfocused);
      }
    }, 10);
  }

  private boolean isHandleSelectionEnabled(@Nonnull KeyType selected, boolean processIfUnfocused) {
    return myEnabled &&
           myComponent.isEnabled() &&
           myComponent.isShowing() &&
           myComponent.getVisibleRect().intersects(getVisibleRect(selected)) &&
           (processIfUnfocused || myComponent.isFocusOwner()) &&
           !isPopup();
  }

  private void doHandleSelectionChange(@Nonnull KeyType selected, boolean processIfUnfocused) {
    if (!isHandleSelectionEnabled(selected, processIfUnfocused)) {
      hideHint();
      return;
    }

    myKey = selected;

    Point location = createToolTipImage(myKey);

    if (location == null) {
      hideHint();
    }
    else {
      Rectangle bounds = new Rectangle(location, myTipComponent.getPreferredSize());
      myPopup.setBounds(bounds);
      myPopup.setVisible(noIntersections(bounds));
      repaintKeyItem();
    }
  }

  protected boolean isPopup() {
    Window window = SwingUtilities.getWindowAncestor(myComponent);
    return window != null
           && !(window instanceof Dialog || window instanceof Frame)
           && !isHintsAllowed(window);
  }

  private static boolean isHintsAllowed(Window window) {
    if (window instanceof RootPaneContainer) {
      final JRootPane pane = ((RootPaneContainer)window).getRootPane();
      if (pane != null) {
        return Boolean.TRUE.equals(pane.getClientProperty(AbstractPopup.SHOW_HINTS));
      }
    }
    return false;
  }

  private boolean noIntersections(Rectangle bounds) {
    Window owner = SwingUtilities.getWindowAncestor(myComponent);
    Window popup = SwingUtilities.getWindowAncestor(myTipComponent);
    Window focus = TargetAWT.to(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow());
    if (focus == owner.getOwner()) {
      focus = null; // do not check intersection with parent
    }
    boolean focused = Platform.current().os().isWindows() || owner.isFocused();
    for (Window other : owner.getOwnedWindows()) {
      if (!focused && !Platform.current().os().isWindows()) {
        focused = other.isFocused();
      }
      if (popup != other && other.isVisible() && bounds.x + 10 >= other.getX() && bounds.intersects(other.getBounds())) {
        return false;
      }
      if (focus == other) {
        focus = null; // already checked
      }
    }
    return focused && (focus == owner || focus == null || !owner.getBounds().intersects(focus.getBounds()));
  }

  private void hideHint() {
    myUpdateAlarm.cancelAllRequests();
    if (myPopup.isVisible()) {
      myPopup.setVisible(false);
      repaintKeyItem();
    }
    myKey = null;
  }

  public boolean isShowing() {
    return myPopup.isVisible();
  }

  private void repaintKeyItem() {
    if (myKeyItemBounds != null) {
      myComponent.repaint(myKeyItemBounds);
    }
  }

  @Nullable
  private Point createToolTipImage(@Nonnull KeyType key) {
    UIUtil.putClientProperty(myComponent, EXPANDED_RENDERER, true);
    Pair<Component, Rectangle> rendererAndBounds = getCellRendererAndBounds(key);
    UIUtil.putClientProperty(myComponent, EXPANDED_RENDERER, null);
    if (rendererAndBounds == null) return null;

    JComponent renderer = ObjectUtil.tryCast(rendererAndBounds.first, JComponent.class);
    if (renderer == null) return null;
    if (UIUtil.isClientPropertyTrue(renderer, RENDERER_DISABLED)) return null;

    if (UIUtil.isClientPropertyTrue(rendererAndBounds.getFirst(), USE_RENDERER_BOUNDS)) {
      rendererAndBounds.getSecond().translate(renderer.getX(), renderer.getY());
      rendererAndBounds.getSecond().setSize(renderer.getSize());
    }

    myKeyItemBounds = rendererAndBounds.second;

    Rectangle cellBounds = myKeyItemBounds;
    Rectangle visibleRect = getVisibleRect(key);

    if (cellBounds.y < visibleRect.y) return null;

    int cellMaxY = cellBounds.y + cellBounds.height;
    int visMaxY = visibleRect.y + visibleRect.height;
    if (cellMaxY > visMaxY) return null;

    int cellMaxX = cellBounds.x + cellBounds.width;
    int visMaxX = visibleRect.x + visibleRect.width;

    Point location = new Point(visMaxX, cellBounds.y);
    SwingUtilities.convertPointToScreen(location, myComponent);

    Rectangle screen = getScreenRectangle(location);

    int borderWidth = isPaintBorder() ? 1 : 0;
    int width = Math.min(screen.width + screen.x - location.x - borderWidth, cellMaxX - visMaxX);
    int height = cellBounds.height;

    if (width <= 0 || height <= 0) return null;

    Dimension size = getImageSize(width, height);
    myImage = UIUtil.createImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = myImage.createGraphics();
    g.setClip(null);
    doFillBackground(height, width, g);
    g.translate(cellBounds.x - visMaxX, 0);
    doPaintTooltipImage(renderer, cellBounds, g, key);

    CustomLineBorder border = null;
    if (borderWidth > 0) {
      border = new CustomLineBorder(getBorderColor(), borderWidth, 0, borderWidth, borderWidth);
      location.y -= borderWidth;
      size.width += borderWidth;
      size.height += borderWidth + borderWidth;
    }

    g.dispose();
    myRendererPane.remove(renderer);

    myTipComponent.setBorder(border);
    myTipComponent.setPreferredSize(size);
    return location;
  }

  public static Rectangle getScreenRectangle(Point location) {
    return !Registry.is("ide.expansion.hints.on.all.screens")
           ? ScreenUtil.getScreenRectangle(location)
           : ScreenUtil.getAllScreensRectangle();
  }

  protected boolean isPaintBorder() {
    return true;
  }

  protected Color getBorderColor() {
    return JBColor.border();
  }

  protected Dimension getImageSize(final int width, final int height) {
    return new Dimension(width, height);
  }

  protected void doFillBackground(int height, int width, Graphics2D g) {
    g.setColor(myComponent.getBackground());
    g.fillRect(0, 0, width, height);
  }

  protected void doPaintTooltipImage(Component rComponent, Rectangle cellBounds, Graphics2D g, KeyType key) {
    myRendererPane.paintComponent(g, rComponent, myComponent, 0, 0, cellBounds.width, cellBounds.height, true);
  }

  protected Rectangle getVisibleRect(KeyType key) {
    return myComponent.getVisibleRect();
  }

  @Nullable
  protected abstract Pair<Component, Rectangle> getCellRendererAndBounds(KeyType key);

  protected abstract KeyType getCellKeyForPoint(Point point);
}