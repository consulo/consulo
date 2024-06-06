// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.application.util.Queryable;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.navigationToolbar.ui.NavBarUIManager;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.ide.impl.idea.ui.ListActions;
import consulo.ide.impl.idea.ui.speedSearch.ListWithFilter;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.platform.Platform;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarPopup extends LightweightHint implements Disposable {
  private static final String JBLIST_KEY = "OriginalList";
  private static final String DISPOSED_OBJECTS = "DISPOSED_OBJECTS";

  private final NavBarPanel myPanel;
  private final int myIndex;

  public NavBarPopup(final NavBarPanel panel, int sourceItemIndex, Object[] siblings, final int selectedIndex) {
    super(createPopupContent(panel, sourceItemIndex, siblings));
    myPanel = panel;
    myIndex = selectedIndex;
    setFocusRequestor(getComponent());
    setForceShowAsPopup(true);
    panel.installPopupHandler(getList(), selectedIndex);
    getList().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        if (Platform.current().os().isWindows()) {
          click(e);
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        if (!Platform.current().os().isWindows()) {
          click(e);
        }
      }

      private void click(final MouseEvent e) {
        if (e.isConsumed()) return;
        myPanel.getModel().setSelectedIndex(selectedIndex);
        if (e.isPopupTrigger()) return;
        Object value = getList().getSelectedValue();
        if (value != null) {
          myPanel.navigateInsideBar(sourceItemIndex, value);
        }
      }
    });
  }

  @Override
  protected void onPopupCancel() {
    final JComponent component = getComponent();
    if (component != null) {
      Object o = component.getClientProperty(JBLIST_KEY);
      if (o instanceof JComponent) HintUpdateSupply.hideHint((JComponent)o);
    }
    //noinspection unchecked
    for (Disposable disposable : (List<? extends Disposable>)getList().getClientProperty(DISPOSED_OBJECTS)) {
      Disposer.dispose(disposable);
    }
    Disposer.dispose(this);
  }

  public void show(final NavBarItem item) {
    show(item, true);
  }

  private void show(final NavBarItem item, boolean checkRepaint) {
    //UIEventLogger.logUIEvent(UIEventId.NavBarShowPopup);

    final RelativePoint point = new RelativePoint(item, new Point(0, item.getHeight()));
    final Point p = point.getPoint(myPanel);
    if (p.x == 0 && p.y == 0 && checkRepaint) { // need repaint of nav bar panel
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        myPanel.getUpdateQueue().rebuildUi();
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> {
          show(item, false); // end-less loop protection
        });
      });
    }
    else {
      int offset = NavBarUIManager.getUI().getPopupOffset(item);
      show(myPanel, p.x - offset, p.y, myPanel, new HintHint(myPanel, p));
      final JBList list = getList();
      AccessibleContextUtil.setName(list, item.getText());
      if (0 <= myIndex && myIndex < list.getItemsCount()) {
        ScrollingUtil.selectItem(list, myIndex);
      }
    }
    if (myPanel.isInFloatingMode()) {
      final Window window = SwingUtilities.windowForComponent(getList());
      window.addWindowFocusListener(new WindowFocusListener() {
        @Override
        public void windowGainedFocus(WindowEvent e) {
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
          final Window w = e.getOppositeWindow();
          if (w != null && DialogWrapper.findInstance(w.getComponent(0)) != null) {
            myPanel.hideHint();
          }
        }
      });
    }
  }

  @Override
  public void dispose() {
  }

  private static JComponent createPopupContent(NavBarPanel panel, int sourceItemIndex, Object[] siblings) {
    class MyList<E> extends JBList<E> implements DataProvider, Queryable {
      @Override
      public void putInfo(@Nonnull Map<String, String> info) {
        panel.putInfo(info);
      }

      @Nullable
      @Override
      public Object getData(@Nonnull Key dataId) {
        return panel.getDataImpl(dataId, this, () -> JBIterable.from(getSelectedValuesList()));
      }
    }
    JBList<Object> list = new MyList<>();
    list.setModel(new CollectionListModel<>(siblings));
    HintUpdateSupply.installSimpleHintUpdateSupply(list);
    List<NavBarItem> items = new ArrayList<>();
    list.putClientProperty(DISPOSED_OBJECTS, items);
    list.installCellRenderer(obj -> {
      for (NavBarItem item : items) {
        if (obj == item.getObject()) {
          item.update();
          return item;
        }
      }
      NavBarItem item = new NavBarItem(panel, obj, null, true);
      items.add(item);
      return item;
    });
    list.setBorder(JBUI.Borders.empty(5));
    ActionMap map = list.getActionMap();
    map.put(ListActions.Left.ID, createMoveAction(panel, -1));
    map.put(ListActions.Right.ID, createMoveAction(panel, 1));
    installEnterAction(list, panel, sourceItemIndex, KeyEvent.VK_ENTER);
    installEscapeAction(list, panel, KeyEvent.VK_ESCAPE);
    JComponent component = ListWithFilter.wrap(list, new NavBarListWrapper(list), o -> panel.getPresentation().getPresentableText(o, false));
    component.putClientProperty(JBLIST_KEY, list);
    return component;
  }

  private static void installEnterAction(JBList list, NavBarPanel panel, int sourceItemIndex, int keyCode) {
    AbstractAction action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        panel.navigateInsideBar(sourceItemIndex, list.getSelectedValue());
      }
    };
    list.registerKeyboardAction(action, KeyStroke.getKeyStroke(keyCode, 0), JComponent.WHEN_FOCUSED);
  }

  private static void installEscapeAction(JBList list, NavBarPanel panel, int keyCode) {
    AbstractAction action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        panel.cancelPopup();
      }
    };
    list.registerKeyboardAction(action, KeyStroke.getKeyStroke(keyCode, 0), JComponent.WHEN_FOCUSED);
  }

  @Nonnull
  public JBList<?> getList() {
    return ((JBList)getComponent().getClientProperty(JBLIST_KEY));
  }

  private static Action createMoveAction(@Nonnull NavBarPanel panel, int direction) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        panel.cancelPopup();
        panel.shiftFocus(direction);
        panel.restorePopup();
      }
    };
  }
}
