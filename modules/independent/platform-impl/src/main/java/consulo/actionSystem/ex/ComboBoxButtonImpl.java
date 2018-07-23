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
import com.intellij.util.ReflectionUtil;
import consulo.ui.SwingUIDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
  private class HackComboBoxPopup implements ComboPopup {
    @Override
    public void show() {
      showPopup0();
    }

    @Override
    public void hide() {
      hidePopup0();
    }

    @Override
    public boolean isVisible() {
      return myCurrentPopupCanceler != null;
    }

    @Override
    public JList getList() {
      return null;
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

  private class HackyComboBoxUI extends ComboBoxUI {

    private BasicComboBoxUI myDelegateUI;

    private MouseListener myMouseListener;

    public HackyComboBoxUI(ComboBoxUI ui) {
      myDelegateUI = (BasicComboBoxUI)ui;
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

        ourPopupField.set(myDelegateUI, new HackComboBoxPopup());

        myMouseListener = new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            showPopup0();
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
        showPopup0();
      }
      else {
        hidePopup0();
      }
    }

    @Override
    public boolean isPopupVisible(JComboBox c) {
      return myCurrentPopupCanceler != null;
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

  private boolean myLikeButton;
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

    if(myLikeButton && myOnClickListener != null) {
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
    return myComboBoxAction.createPopup(getDataContext(), onDispose);
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
  public void removeNotify() {
    if (myButtonSynchronizer != null) {
      myPresentation.removePropertyChangeListener(myButtonSynchronizer);
      myButtonSynchronizer = null;
    }
    super.removeNotify();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myButtonSynchronizer == null) {
      myButtonSynchronizer = new MyButtonSynchronizer();
      myPresentation.addPropertyChangeListener(myButtonSynchronizer);
    }
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
        setLikeButton(true, (Runnable)evt.getNewValue());
      }
    }
  }

  private void updateTooltipText(String description) {
    String tooltip = KeymapUtil.createTooltipText(description, myComboBoxAction);
    setToolTipText(!tooltip.isEmpty() ? tooltip : null);
  }

  @Override
  public void updateUI() {
    ComboBoxUI comboBoxUI = (ComboBoxUI)UIManager.getUI(this);

    setUI(new HackyComboBoxUI(comboBoxUI));

    // refresh state
    setLikeButton(myLikeButton, myOnClickListener);

    SwingUIDecorator.apply(SwingUIDecorator::decorateToolbarComboBox, this);
  }

  @Nonnull
  @Override
  public ComboBoxAction getComboBoxAction() {
    return myComboBoxAction;
  }

  @Override
  public void setLikeButton(boolean value, @Nullable Runnable onClick) {
    myLikeButton = value;
    myOnClickListener = onClick;
  }
}
