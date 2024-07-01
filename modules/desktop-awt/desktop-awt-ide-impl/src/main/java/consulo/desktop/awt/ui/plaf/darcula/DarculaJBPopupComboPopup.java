// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * @author gregsh
 */
//@ApiStatus.Experimental
public class DarculaJBPopupComboPopup<T> implements ComboPopup, ItemListener, MouseListener, MouseMotionListener, MouseWheelListener, PropertyChangeListener, AncestorListener {

  public static final String CLIENT_PROP = "ComboBox.jbPopup";

  private final JComboBox<T> myComboBox;
  private final JList<T> myProxyList = new JBList<>();
  private ListPopupImpl myPopup;
  private boolean myJustClosedViaClick;

  public DarculaJBPopupComboPopup(@Nonnull JComboBox<T> comboBox) {
    myComboBox = comboBox;
    myProxyList.setModel(comboBox.getModel());
    myComboBox.addPropertyChangeListener(this);
    myComboBox.addItemListener(this);
    myComboBox.addAncestorListener(this);
  }

  @Override
  public void show() {
    myJustClosedViaClick = false;
    if (myPopup != null) {
      if (myPopup.isVisible()) return;
      // onClosed() was not called for some reason
      myPopup.cancel();
    }

    ArrayList<T> items = new ArrayList<>(myComboBox.getModel().getSize());
    for (int i = 0, size = myComboBox.getModel().getSize(); i < size; i++) {
      items.add(myComboBox.getModel().getElementAt(i));
    }
    BaseListPopupStep<T> step = new BaseListPopupStep<T>("", items) {
      @Nullable
      @Override
      public PopupStep onChosen(T selectedValue, boolean finalChoice) {
        myComboBox.setSelectedItem(selectedValue);
        return FINAL_CHOICE;
      }

      @Override
      public void canceled() {
        myComboBox.firePopupMenuCanceled();
      }

      @Override
      public boolean isSpeedSearchEnabled() {
        return true;
      }

      @Nonnull
      @Override
      public String getTextFor(T value) {
        Component component = myComboBox.getRenderer().getListCellRendererComponent(myProxyList, value, -1, false, false);
        return component instanceof TitledSeparator || component instanceof JSeparator
               ? ""
               : component instanceof JLabel
                 ? ((JLabel)component).getText()
                 : component instanceof SimpleColoredComponent ? ((SimpleColoredComponent)component).getCharSequence(false).toString() : String.valueOf(value);
      }

      @Override
      public boolean isSelectable(T value) {
        Component component = myComboBox.getRenderer().getListCellRendererComponent(myProxyList, value, -1, false, false);
        return !(component instanceof TitledSeparator || component instanceof JSeparator);
      }
    };
    step.setDefaultOptionIndex(myComboBox.getSelectedIndex());
    Project project = DataManager.getInstance().getDataContext(myComboBox).getData(Project.KEY);
    myPopup = new ListPopupImpl(project, step) {
      @Override
      public void cancel(InputEvent e) {
        if (e instanceof MouseEvent) {
          // we want the second click on combo-box just to close
          // and not to instantly show the popup again in the following
          // DarculaJBPopupComboPopup#mousePressed()
          Point point = new RelativePoint((MouseEvent)e).getPoint(myComboBox);
          myJustClosedViaClick = new Rectangle(myComboBox.getSize()).contains(point);
        }
        super.cancel(e);
      }
    };
    myPopup.setMaxRowCount(10);
    myPopup.setRequestFocus(false);
    myPopup.addListener(new JBPopupListener() {
      @Override
      public void beforeShown(@Nonnull LightweightWindowEvent event) {
        myComboBox.firePopupMenuWillBecomeVisible();
      }

      @Override
      public void onClosed(@Nonnull LightweightWindowEvent event) {
        myComboBox.firePopupMenuWillBecomeInvisible();
        myPopup = null;
        myProxyList.setCellRenderer(new DefaultListCellRenderer());
        myProxyList.setModel(myComboBox.getModel());
      }
    });
    //noinspection unchecked
    JList<T> list = myPopup.getList();
    configureList(list);
    Border border = UIManager.getBorder("ComboPopup.border");
    if (border != null) {
      myPopup.getContent().setBorder(border);
    }

    myProxyList.setCellRenderer(list.getCellRenderer());
    myProxyList.setModel(list.getModel());
    myPopup.setMinimumSize(myComboBox.getSize());
    myPopup.showUnderneathOf(myComboBox);
  }

  protected void configureList(@Nonnull JList<T> list) {
    list.setFont(myComboBox.getFont());
    list.setForeground(myComboBox.getForeground());
    list.setBackground(myComboBox.getBackground());
    list.setSelectionForeground(UIManager.getColor("ComboBox.selectionForeground"));
    list.setSelectionBackground(UIManager.getColor("ComboBox.selectionBackground"));
    list.setBorder(null);
    //noinspection unchecked
    list.setCellRenderer(new MyDelegateRenderer());
    list.setFocusable(false);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  protected void customizeListRendererComponent(JComponent component) {
    component.setBorder(JBUI.Borders.empty(2, 8));
  }

  @Override
  public void hide() {
    myJustClosedViaClick = false;
    if (myPopup == null) return;
    myPopup.cancel();
  }

  @Override
  public boolean isVisible() {
    return myPopup != null && myPopup.isVisible();
  }

  @Override
  public JList getList() {
    return myProxyList;
  }

  @Override
  public MouseListener getMouseListener() {
    return this;
  }

  @Override
  public MouseMotionListener getMouseMotionListener() {
    return this;
  }

  @Override
  public KeyListener getKeyListener() {
    return null;
  }

  @Override
  public void uninstallingUI() {
    myComboBox.removePropertyChangeListener(this);
    myComboBox.removeItemListener(this);
    myComboBox.removeAncestorListener(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent e) {
    String propertyName = e.getPropertyName();
    if ("model".equals(propertyName) || "renderer".equals(propertyName) || "editable".equals(propertyName)) {
      if (isVisible()) {
        hide();
      }
    }
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (e.getSource() == getList()) return;
    if (!SwingUtilities.isLeftMouseButton(e) || !myComboBox.isEnabled()) return;

    if (myComboBox.isEditable()) {
      Component comp = myComboBox.getEditor().getEditorComponent();
      if ((!(comp instanceof JComponent)) || ((JComponent)comp).isRequestFocusEnabled()) {
        comp.requestFocus();
      }
    }
    else if (myComboBox.isRequestFocusEnabled()) {
      myComboBox.requestFocus();
    }
    if (myJustClosedViaClick) {
      myJustClosedViaClick = false;
      return;
    }
    if (isVisible()) {
      hide();
    }
    else {
      show();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  @Override
  public void mouseDragged(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
  }

  @Override
  public void ancestorAdded(AncestorEvent event) {

  }

  @Override
  public void ancestorRemoved(AncestorEvent event) {

  }

  @Override
  public void ancestorMoved(AncestorEvent event) {
    hide();
  }

  private class MyDelegateRenderer implements ListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      //noinspection unchecked
      Component component = myComboBox.getRenderer().getListCellRendererComponent(list, (T)value, index, isSelected, cellHasFocus);
      if (component instanceof JComponent && !(component instanceof JSeparator || component instanceof TitledSeparator)) {
        customizeListRendererComponent((JComponent)component);
      }
      return component;
    }
  }
}
