/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.annotation.DeprecationInfo;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.component.util.Weighted;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ide.impl.idea.openapi.ui.impl.GlassPaneDialogWrapperPeer;
import consulo.ide.impl.idea.ui.BalloonImpl;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.logging.Logger;
import consulo.ui.ex.IdeGlassPane;
import consulo.ui.ex.Painter;
import consulo.ui.ex.awt.Divider;
import consulo.ui.ex.awt.EmptyClipboardOwner;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.dnd.DnDAware;
import consulo.ui.ex.awt.event.MouseEventAdapter;
import consulo.ui.ex.awt.util.IdeGlassPaneUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.FactoryMap;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class IdeGlassPaneImpl extends JPanel implements IdeGlassPaneEx, Predicate<AWTEvent> {

  private static final Logger LOG = Logger.getInstance(IdeGlassPaneImpl.class);
  private static final String PREPROCESSED_CURSOR_KEY = "SuperCursor";

  private final List<EventListener> myMouseListeners = new ArrayList<>();
  private final Set<EventListener> mySortedMouseListeners = new TreeSet<>((o1, o2) -> {
    double weight1 = 0;
    double weight2 = 0;
    if (o1 instanceof Weighted) {
      weight1 = ((Weighted)o1).getWeight();
    }
    if (o2 instanceof Weighted) {
      weight2 = ((Weighted)o2).getWeight();
    }
    return weight1 > weight2 ? 1 : weight1 < weight2 ? -1 : myMouseListeners.indexOf(o1) - myMouseListeners.indexOf(o2);
  });
  private final JRootPane myRootPane;

  private final Map<String, PaintersHelper> myNamedPainters = FactoryMap.create(it -> new PaintersHelper(this));

  private boolean myPreprocessorActive;
  private final Map<Object, Cursor> myListener2Cursor = new LinkedHashMap<>();

  private Component myLastCursorComponent;
  private Cursor myLastOriginalCursor;
  private MouseEvent myPrevPressEvent;

  public IdeGlassPaneImpl(JRootPane rootPane) {
    myRootPane = rootPane;
    setOpaque(false);
    setVisible(false);
    setLayout(null);
  }

  @Deprecated
  @DeprecationInfo("installPainters parameter not used. use contructor without it")
  public IdeGlassPaneImpl(JRootPane rootPane, boolean installPainters) {
    myRootPane = rootPane;
    setOpaque(false);
    setVisible(false);
    setLayout(null);
  }

  @Override
  public void addNotify() {
    super.addNotify();
  }

  @Override
  public boolean test(@Nonnull final AWTEvent e) {
    JRootPane eventRootPane = myRootPane;

    if (e instanceof MouseEvent) {
      MouseEvent me = (MouseEvent)e;
      Window eventWindow = UIUtil.getWindow(me.getComponent());

      if (isContextMenu(eventWindow)) return false;

      final Window thisGlassWindow = SwingUtilities.getWindowAncestor(myRootPane);

      if (eventWindow instanceof JWindow) {
        eventRootPane = ((JWindow)eventWindow).getRootPane();
        if (eventRootPane != null) {
          if (!(eventRootPane.getGlassPane() instanceof IdeGlassPane)) {
            final Container parentWindow = eventWindow.getParent();
            if (parentWindow instanceof Window) {
              eventWindow = (Window)parentWindow;
            }
          }
        }
      }

      if (eventWindow != thisGlassWindow) return false;
    }


    if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
      if (ApplicationManager.getApplication() != null) {
        IdeTooltipManagerImpl.getInstanceImpl().hideCurrent((MouseEvent)e);
      }
    }

    boolean dispatched;
    if (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MouseEvent.MOUSE_CLICKED) {
      dispatched = preprocess((MouseEvent)e, false, eventRootPane);
    }
    else if (e.getID() == MouseEvent.MOUSE_MOVED || e.getID() == MouseEvent.MOUSE_DRAGGED) {
      dispatched = preprocess((MouseEvent)e, true, eventRootPane);
    }
    else if (e.getID() == MouseEvent.MOUSE_EXITED || e.getID() == MouseEvent.MOUSE_ENTERED) {
      dispatched = preprocess((MouseEvent)e, false, eventRootPane);
    }
    else {
      return false;
    }

    MouseEvent me = (MouseEvent)e;
    final Component meComponent = me.getComponent();
    if (!dispatched && meComponent != null) {
      final Window eventWindow = UIUtil.getWindow(meComponent);
      if (eventWindow != SwingUtilities.getWindowAncestor(myRootPane)) {
        return false;
      }
      int button1 = MouseEvent.BUTTON1_MASK | MouseEvent.BUTTON1_DOWN_MASK;
      final boolean pureMouse1Event = (me.getModifiersEx() | button1) == button1;
      if (pureMouse1Event && me.getClickCount() <= 1 && !me.isPopupTrigger()) {
        final Point point = SwingUtilities.convertPoint(meComponent, me.getPoint(), myRootPane.getContentPane());
        JMenuBar menuBar = myRootPane.getJMenuBar();
        point.y += menuBar != null ? menuBar.getHeight() : 0;

        final Component target = SwingUtilities.getDeepestComponentAt(myRootPane.getContentPane().getParent(), point.x, point.y);
        if (target instanceof DnDAware) {
          final Point targetPoint = SwingUtilities.convertPoint(myRootPane.getContentPane().getParent(), point.x, point.y, target);
          final boolean overSelection = ((DnDAware)target).isOverSelection(targetPoint);
          if (overSelection) {
            final MouseListener[] listeners = target.getListeners(MouseListener.class);
            final MouseEvent mouseEvent = MouseEventAdapter.convert(me, target);
            switch (me.getID()) {
              case MouseEvent.MOUSE_PRESSED:
                boolean consumed = false;
                if (target.isFocusable()) {
                  IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                    IdeFocusManager.getGlobalInstance().requestFocus(target, true);
                  });
                }
                for (final MouseListener listener : listeners) {
                  final String className = listener.getClass().getName();
                  if (className.indexOf("BasicTreeUI$") >= 0 || className.indexOf("MacTreeUI$") >= 0) continue;
                  fireMouseEvent(listener, mouseEvent);
                  if (mouseEvent.isConsumed()) {
                    consumed = true;
                    break;
                  }
                }

                if (!mouseEvent.isConsumed()) {
                  final AWTEventListener[] eventListeners = Toolkit.getDefaultToolkit().getAWTEventListeners(MouseEvent.MOUSE_EVENT_MASK);
                  if (eventListeners != null && eventListeners.length > 0) {
                    for (final AWTEventListener eventListener : eventListeners) {
                      eventListener.eventDispatched(me);
                      if (me.isConsumed()) break;
                    }

                    if (me.isConsumed()) {
                      consumed = true;
                      break;
                    }
                  }
                }

                if (!consumed) {
                  myPrevPressEvent = mouseEvent;
                }
                else {
                  me.consume();
                }

                dispatched = true;
                break;
              case MouseEvent.MOUSE_RELEASED:
                if (myPrevPressEvent != null && myPrevPressEvent.getComponent() == target) {
                  for (final MouseListener listener : listeners) {
                    final String className = listener.getClass().getName();
                    if (className.indexOf("BasicTreeUI$") >= 0 || className.indexOf("MacTreeUI$") >= 0) {
                      fireMouseEvent(listener, myPrevPressEvent);
                      fireMouseEvent(listener, mouseEvent);
                      if (mouseEvent.isConsumed()) {
                        break;
                      }
                    }

                    fireMouseEvent(listener, mouseEvent);
                    if (mouseEvent.isConsumed()) {
                      break;
                    }
                  }

                  if (mouseEvent.isConsumed()) {
                    me.consume();
                  }

                  myPrevPressEvent = null;
                  dispatched = true;
                }
                break;
              default:
                myPrevPressEvent = null;
                break;
            }
          }
        }
      }
    }


    if (isVisible() && getComponentCount() == 0) {
      boolean cursorSet = false;
      if (meComponent != null) {
        final Point point = SwingUtilities.convertPoint(meComponent, me.getPoint(), myRootPane.getContentPane().getParent());

        if (myRootPane.getMenuBar() != null && myRootPane.getMenuBar().isVisible()) {
          point.y += myRootPane.getMenuBar().getHeight();
        }

        final Component target = SwingUtilities.getDeepestComponentAt(myRootPane.getContentPane().getParent(), point.x, point.y);
        if (target != null) {
          setCursor(target.getCursor());
          cursorSet = true;
        }
      }

      if (!cursorSet) {
        setCursor(Cursor.getDefaultCursor());
      }
    }

    return dispatched;
  }

  private static boolean isContextMenu(Window window) {
    if (window != null) {
      for (Component component : window.getComponents()) {
        if (component instanceof JComponent && UIUtil.findComponentOfType((JComponent)component, JPopupMenu.class) != null) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean preprocess(final MouseEvent e, final boolean motion, JRootPane eventRootPane) {
    try {
      if (UIUtil.getWindow(this) != UIUtil.getWindow(e.getComponent())) return false;

      final MouseEvent event = MouseEventAdapter.convert(e, eventRootPane);
      if (event.isAltDown() && SwingUtilities.isLeftMouseButton(event) && event.getID() == MouseEvent.MOUSE_PRESSED) {
        Component c = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
        Balloon balloon = JBPopupFactory.getInstance().getParentBalloonFor(c);
        if (balloon instanceof BalloonImpl) {
          JComponent component = ((BalloonImpl)balloon).getComponent();
          component.getToolkit().getSystemClipboard().setContents(new StringSelection(UIUtil.getDebugText(component)), EmptyClipboardOwner.INSTANCE);
        }
      }

      if (!IdeGlassPaneUtil.canBePreprocessed(e)) {
        return false;
      }

      for (EventListener each : mySortedMouseListeners) {
        if (motion && each instanceof MouseMotionListener) {
          fireMouseMotion((MouseMotionListener)each, event);
        }
        else if (!motion && each instanceof MouseListener) {
          fireMouseEvent((MouseListener)each, event);
        }

        if (event.isConsumed()) {
          e.consume();
          return true;
        }
      }

      return false;
    }
    finally {
      if (eventRootPane == myRootPane) {
        Cursor cursor;
        if (!myListener2Cursor.isEmpty()) {
          cursor = myListener2Cursor.values().iterator().next();

          Container parent = myRootPane.getContentPane().getParent();
          final Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent);
          Component target = SwingUtilities.getDeepestComponentAt(parent, point.x, point.y);

          if (canProcessCursorFor(target)) {
            target = getCompWithCursor(target);

            restoreLastComponent(target);

            if (target != null) {
              if (myLastCursorComponent != target) {
                myLastCursorComponent = target;
                myLastOriginalCursor = target.getCursor();
              }

              if (cursor != null && !cursor.equals(target.getCursor())) {
                if (target instanceof JComponent) {
                  ((JComponent)target).putClientProperty(PREPROCESSED_CURSOR_KEY, Boolean.TRUE);
                }
                target.setCursor(cursor);
              }
            }

            getRootPane().setCursor(cursor);
          }
        }
        else if (!e.isConsumed() && e.getID() != MouseEvent.MOUSE_DRAGGED) {
          cursor = Cursor.getDefaultCursor();
          JRootPane rootPane = getRootPane();
          if (rootPane != null) {
            rootPane.setCursor(cursor);
          }
          else {
            LOG.warn("Root pane is null. Event: " + e);
          }
          restoreLastComponent(null);
          myLastOriginalCursor = null;
          myLastCursorComponent = null;
        }
        myListener2Cursor.clear();
      }
    }
  }

  private boolean canProcessCursorFor(Component target) {
    if (target instanceof JMenu ||
        target instanceof JMenuItem ||
        target instanceof Divider ||
        target instanceof JSeparator ||
        (target instanceof JEditorPane && ((JEditorPane)target).getEditorKit() instanceof HTMLEditorKit)) {
      return false;
    }
    return true;
  }

  private Component getCompWithCursor(Component c) {
    Component eachParentWithCursor = c;
    while (eachParentWithCursor != null) {
      if (eachParentWithCursor.isCursorSet()) return eachParentWithCursor;
      eachParentWithCursor = eachParentWithCursor.getParent();
    }

    return null;
  }

  private void restoreLastComponent(Component newC) {
    if (myLastCursorComponent != null && myLastCursorComponent != newC) {
      myLastCursorComponent.setCursor(myLastOriginalCursor);
      if (myLastCursorComponent instanceof JComponent) {
        ((JComponent)myLastCursorComponent).putClientProperty(PREPROCESSED_CURSOR_KEY, null);
      }
    }
  }

  public static boolean hasPreProcessedCursor(@Nonnull JComponent component) {
    return component.getClientProperty(PREPROCESSED_CURSOR_KEY) != null;
  }

  public static boolean savePreProcessedCursor(@Nonnull JComponent component, @Nonnull Cursor cursor) {
    if (hasPreProcessedCursor(component)) {
      return false;
    }

    component.putClientProperty(PREPROCESSED_CURSOR_KEY, cursor);
    return true;
  }

  @Override
  public void setCursor(Cursor cursor, @Nonnull Object requestor) {
    if (cursor == null) {
      myListener2Cursor.remove(requestor);
    }
    else {
      myListener2Cursor.put(requestor, cursor);
    }
  }

  private static void fireMouseEvent(final MouseListener listener, final MouseEvent event) {
    switch (event.getID()) {
      case MouseEvent.MOUSE_PRESSED:
        listener.mousePressed(event);
        break;
      case MouseEvent.MOUSE_RELEASED:
        listener.mouseReleased(event);
        break;
      case MouseEvent.MOUSE_ENTERED:
        listener.mouseEntered(event);
        break;
      case MouseEvent.MOUSE_EXITED:
        listener.mouseExited(event);
        break;
      case MouseEvent.MOUSE_CLICKED:
        listener.mouseClicked(event);
        break;
    }
  }

  private static void fireMouseMotion(MouseMotionListener listener, final MouseEvent event) {
    switch (event.getID()) {
      case MouseEvent.MOUSE_DRAGGED:
        listener.mouseDragged(event);
      case MouseEvent.MOUSE_MOVED:
        listener.mouseMoved(event);
    }
  }

  @Override
  public void addMousePreprocessor(final MouseListener listener, Disposable parent) {
    _addListener(listener, parent);
  }

  @Override
  public void addMouseMotionPreprocessor(final MouseMotionListener listener, final Disposable parent) {
    _addListener(listener, parent);
  }

  private void _addListener(final EventListener listener, final Disposable parent) {
    if (!myMouseListeners.contains(listener)) {
      myMouseListeners.add(listener);
      updateSortedList();
    }
    activateIfNeeded();
    Disposer.register(parent, () -> UIUtil.invokeLaterIfNeeded(() -> removeListener(listener)));
  }

  @Override
  public void removeMousePreprocessor(final MouseListener listener) {
    removeListener(listener);
  }

  @Override
  public void removeMouseMotionPreprocessor(final MouseMotionListener listener) {
    removeListener(listener);
  }

  private void removeListener(final EventListener listener) {
    if (myMouseListeners.remove(listener)) {
      updateSortedList();
    }
    deactivateIfNeeded();
  }

  private void updateSortedList() {
    mySortedMouseListeners.clear();
    mySortedMouseListeners.addAll(myMouseListeners);
  }

  private void deactivateIfNeeded() {
    if (myPreprocessorActive && myMouseListeners.isEmpty()) {
      myPreprocessorActive = false;
    }

    applyActivationState();
  }

  private void activateIfNeeded() {
    if (!myPreprocessorActive && !myMouseListeners.isEmpty()) {
      myPreprocessorActive = true;
    }

    applyActivationState();
  }

  private void applyActivationState() {
    boolean wasVisible = isVisible();
    boolean hasWork = getPainters().hasPainters() || getComponentCount() > 0;

    if (wasVisible != hasWork) {
      setVisible(hasWork);
    }

    IdeEventQueueProxy queue = IdeEventQueueProxy.getInstance();
    if (!queue.containsDispatcher(this) && (myPreprocessorActive || isVisible())) {
      queue.addDispatcher(this, null);
    }
    else if (queue.containsDispatcher(this) && !myPreprocessorActive && !isVisible()) {
      queue.removeDispatcher(this);
    }

    if (wasVisible != isVisible()) {
      revalidate();
      repaint();
    }
  }

  @Nonnull
  PaintersHelper getNamedPainters(@Nonnull String name) {
    return myNamedPainters.get(name);
  }

  @Nonnull
  private PaintersHelper getPainters() {
    return getNamedPainters("glass");
  }

  @Override
  public void addPainter(final Component component, final Painter painter, final Disposable parent) {
    getPainters().addPainter(painter, component);
    activateIfNeeded();
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        SwingUtilities.invokeLater(() -> removePainter(painter));
      }
    });
  }

  @Override
  public void removePainter(final Painter painter) {
    getPainters().removePainter(painter);
    deactivateIfNeeded();
  }


  @Override
  protected void addImpl(Component comp, Object constraints, int index) {
    super.addImpl(comp, constraints, index);

    SwingUtilities.invokeLater(() -> activateIfNeeded());
  }

  @Override
  public void remove(final Component comp) {
    super.remove(comp);

    SwingUtilities.invokeLater(() -> deactivateIfNeeded());
  }

  @Override
  public boolean isInModalContext() {
    final Component[] components = getComponents();
    for (Component component : components) {
      if (component instanceof GlassPaneDialogWrapperPeer.TransparentLayeredPane) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    getPainters().paint(g);
  }

  @Override
  protected void paintChildren(Graphics g) {
    super.paintChildren(g);
  }

  public Component getTargetComponentFor(MouseEvent e) {
    Component candidate = findComponent(e, myRootPane.getLayeredPane());
    if (candidate != null) return candidate;
    candidate = findComponent(e, myRootPane.getContentPane());
    if (candidate != null) return candidate;
    return e.getComponent();
  }

  private static Component findComponent(final MouseEvent e, final Container container) {
    final Point lpPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), container);
    return SwingUtilities.getDeepestComponentAt(container, lpPoint.x, lpPoint.y);
  }

  @Override
  public boolean isOptimizedDrawingEnabled() {
    return !getPainters().hasPainters() && super.isOptimizedDrawingEnabled();
  }
}
