/*
 * Copyright 2013-2018 consulo.io
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
package consulo.actionSystem.ex;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.FieldAccessor;
import com.intellij.util.ObjectUtil;
import com.intellij.util.ReflectionUtil;
import consulo.ui.SwingUIDecorator;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 2018-07-12
 * <p>
 * Component which looks like ComboBox but will show action popup.
 * How it works:
 * 1. UI painter hacked. We get laf ui instance - then, override popup field
 * 2. Adding items dont supported. There only one item, which used for rendering
 */
public final class ComboBoxButtonImpl extends JComboBox<Object> implements ComboBoxButton {
  public static interface ComboBoxUIFactory {
    @Nonnull
    HackyComboBoxUI create(@Nonnull ComboBoxUI delegate, @Nonnull ComboBoxButtonImpl comboBoxButton);
  }

  private static class DefaultComboBoxUIFactory implements ComboBoxUIFactory {
    private static final DefaultComboBoxUIFactory INSTANCE = new DefaultComboBoxUIFactory();

    @Nonnull
    @Override
    public HackyComboBoxUI create(@Nonnull ComboBoxUI delegate, @Nonnull ComboBoxButtonImpl comboBoxButton) {
      return new HackyComboBoxUI(delegate, comboBoxButton);
    }
  }

  public static class HackComboBoxPopup implements ComboPopup {
    private final JList<?> myDummyList = new JList<>();
    private final ComboBoxButtonImpl myButton;

    HackComboBoxPopup(ComboBoxButtonImpl button) {
      myButton = button;
      // some ui register listeners to JList of popup
      // just return dummy instance
      // also override default UI since, some ui like Aqua can just skip list if is not aqua list ui
      myDummyList.setUI(new BasicListUI());
    }

    @Override
    public void show() {
      myButton.showPopup0();
    }

    @Override
    public void hide() {
      myButton.hidePopup0();
    }

    @Override
    public boolean isVisible() {
      return myButton.myCurrentPopupCanceler != null;
    }

    @Override
    public JList getList() {
      return myDummyList;
    }

    @Override
    public MouseListener getMouseListener() {
      return new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          show();
        }
      };
    }

    @Override
    public MouseMotionListener getMouseMotionListener() {
      return null;
    }

    @Override
    public KeyListener getKeyListener() {
      return null;
    }

    @Override
    public void uninstallingUI() {
    }
  }

  private static Field ourPopupField = ReflectionUtil.getDeclaredField(BasicComboBoxUI.class, "popup");

  private static Field popupMouseListener = ReflectionUtil.getDeclaredField(BasicComboBoxUI.class, "popupMouseListener");
  private static Field popupMouseMotionListener = ReflectionUtil.getDeclaredField(BasicComboBoxUI.class, "popupMouseMotionListener");
  private static Field popupKeyListener = ReflectionUtil.getDeclaredField(BasicComboBoxUI.class, "popupKeyListener");
  private static FieldAccessor<BasicComboBoxUI, JButton> arrowButton = new FieldAccessor<>(BasicComboBoxUI.class, "arrowButton");

  public static class HackyComboBoxUI extends ComboBoxUI {

    protected final ComboBoxButtonImpl myButton;
    protected final BasicComboBoxUI myDelegateUI;

    private MouseListener myMouseListener;

    public HackyComboBoxUI(ComboBoxUI ui, ComboBoxButtonImpl button) {
      myButton = button;
      myDelegateUI = (BasicComboBoxUI)ui;
    }

    @Override
    public Accessible getAccessibleChild(JComponent c, int i) {
      return null;
    }

    @Override
    public void installUI(JComponent c) {
      myDelegateUI.installUI(c);

      try {
        // unregister native popup
        ComboPopup o = (ComboPopup)ourPopupField.get(myDelegateUI);
        if (o != null) {
          o.uninstallingUI();

          myDelegateUI.unconfigureArrowButton();

          KeyListener keyListener = (KeyListener)popupKeyListener.get(myDelegateUI);
          if (keyListener != null) {
            c.removeKeyListener(keyListener);

            popupKeyListener.set(myDelegateUI, new KeyAdapter() {
            });
          }

          MouseListener mouseListener = (MouseListener)popupMouseListener.get(myDelegateUI);
          if (mouseListener != null) {
            c.removeMouseListener(mouseListener);

            popupMouseListener.set(myDelegateUI, new MouseAdapter() {
            });
          }

          MouseMotionListener mouseMotionListener = (MouseMotionListener)popupMouseMotionListener.get(myDelegateUI);
          if (mouseMotionListener != null) {
            c.removeMouseMotionListener(mouseMotionListener);

            popupMouseMotionListener.set(myDelegateUI, new MouseMotionAdapter() {
            });
          }
        }

        ourPopupField.set(myDelegateUI, new HackComboBoxPopup(myButton));

        myMouseListener = new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            myButton.showPopup0();
          }
        };

        c.addMouseListener(myMouseListener);

        myDelegateUI.configureArrowButton();
      }
      catch (IllegalAccessException e) {
        throw new Error(e);
      }
    }

    @Override
    public void uninstallUI(JComponent c) {
      myDelegateUI.uninstallUI(c);
      c.removeMouseListener(myMouseListener);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
      myDelegateUI.paint(g, c);
    }

    @Override
    public void update(Graphics g, JComponent c) {
      myDelegateUI.update(g, c);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
      return myDelegateUI.getPreferredSize(c);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
      return myDelegateUI.getMinimumSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
      return myDelegateUI.getMaximumSize(c);
    }

    @Override
    public void setPopupVisible(JComboBox c, boolean v) {
      if (v) {
        myButton.showPopup0();
      }
      else {
        myButton.hidePopup0();
      }
    }

    @Override
    public boolean isPopupVisible(JComboBox c) {
      return myButton.myCurrentPopupCanceler != null;
    }

    @Override
    public boolean isFocusTraversable(JComboBox c) {
      return myDelegateUI.isFocusTraversable(c);
    }
  }

  private final ComboBoxAction myComboBoxAction;
  private final Presentation myPresentation;

  private Runnable myCurrentPopupCanceler;
  private PropertyChangeListener myButtonSynchronizer;

  private Runnable myOnClickListener;

  public ComboBoxButtonImpl(ComboBoxAction comboBoxAction, Presentation presentation) {
    myComboBoxAction = comboBoxAction;
    myPresentation = presentation;

    setRenderer(new ColoredListCellRenderer<Object>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
        append(StringUtil.notNullize(myPresentation.getText()));
        setIcon(myPresentation.getIcon());
      }
    });

    // add and select one value
    revalidateValue();
    updateSize();
    updateTooltipText(presentation.getDescription());
  }

  private void revalidateValue() {
    Object oldValue = getSelectedItem();

    Object value = new Object();
    addItem(value);
    setSelectedItem(value);

    if (oldValue != null) {
      removeItem(oldValue);
    }
  }

  private void hidePopup0() {
    if (myCurrentPopupCanceler != null) {
      myCurrentPopupCanceler.run();
      myCurrentPopupCanceler = null;
    }
  }

  private void showPopup0() {
    hidePopup0();

    if (myOnClickListener != null) {
      myOnClickListener.run();

      myCurrentPopupCanceler = null;
      return;
    }

    JBPopup popup = createPopup(() -> {
      myCurrentPopupCanceler = null;

      updateSize();
    });
    popup.showUnderneathOf(this);

    myCurrentPopupCanceler = popup::cancel;
  }

  private JBPopup createPopup(Runnable onDispose) {
    return myComboBoxAction.createPopup(this, getDataContext(), onDispose);
  }

  protected void updateSize() {
    revalidateValue();
    setSize(getPreferredSize());

    invalidate();
    repaint();
  }

  protected DataContext getDataContext() {
    return DataManager.getInstance().getDataContext(this);
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myButtonSynchronizer == null) {
      myButtonSynchronizer = new MyButtonSynchronizer();
      myPresentation.addPropertyChangeListener(myButtonSynchronizer);
      myPresentation.fireAllProperties();
    }
  }

  @Override
  public void removeNotify() {
    if (myButtonSynchronizer != null) {
      myPresentation.removePropertyChangeListener(myButtonSynchronizer);
      myButtonSynchronizer = null;
    }
    super.removeNotify();
  }

  private class MyButtonSynchronizer implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      String propertyName = evt.getPropertyName();
      if (Presentation.PROP_TEXT.equals(propertyName)) {
        updateSize();
      }
      else if (Presentation.PROP_DESCRIPTION.equals(propertyName)) {
        updateTooltipText((String)evt.getNewValue());
      }
      else if (Presentation.PROP_ICON.equals(propertyName)) {
        updateSize();
      }
      else if (Presentation.PROP_ENABLED.equals(propertyName)) {
        setEnabled(((Boolean)evt.getNewValue()).booleanValue());
      }
      else if (ComboBoxButton.LIKE_BUTTON.equals(propertyName)) {
        setLikeButton((Runnable)evt.getNewValue());
      }
    }
  }

  private void updateTooltipText(String description) {
    String tooltip = KeymapUtil.createTooltipText(description, myComboBoxAction);
    setToolTipText(!tooltip.isEmpty() ? tooltip : null);
  }

  @Override
  public void updateUI() {
    ComboBoxUIFactory factory = ObjectUtil.notNull((ComboBoxUIFactory)UIManager.get(ComboBoxUIFactory.class), DefaultComboBoxUIFactory.INSTANCE);

    ComboBoxUI delegate = (ComboBoxUI)UIManager.getUI(this);

    setUI(factory.create(delegate, this));

    // refresh state
    setLikeButton(myOnClickListener);

    SwingUIDecorator.apply(SwingUIDecorator::decorateToolbarComboBox, this);
  }

  @Nonnull
  @Override
  public ComboBoxAction getComboBoxAction() {
    return myComboBoxAction;
  }

  private void setLikeButton(@Nullable Runnable onClick) {
    myOnClickListener = onClick;
  }
}
