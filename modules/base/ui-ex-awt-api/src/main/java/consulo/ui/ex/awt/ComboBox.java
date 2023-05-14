/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.util.SystemInfo;
import consulo.awt.hacking.BasicComboBoxUIHacking;
import consulo.ui.ex.awt.util.MacUIUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Due to many bugs and "features" in <code>JComboBox</code> implementation we provide
 * our own "patch". First of all it has correct preferred and minimum sizes that has sense
 * when combo box is editable. Also this implementation fixes some bugs with clicking
 * of default button. The SUN's combo box eats first "Enter" if the selected value from
 * the list and changed it. They say that combo box "commit" changes and only second
 * "Enter" clicks default button. This implementation clicks the default button
 * immediately. As the result of our patch combo box has internal wrapper for ComboBoxEditor.
 * It means that <code>getEditor</code> method always returns not the same value you set
 * by <code>setEditor</code> method. Moreover adding and removing of action listeners
 * isn't supported by the implementation of wrapper.
 *
 * @author Vladimir Kondratyev
 */
public class ComboBox<E> extends ComboBoxWithWidePopup<E> implements AWTEventListener {
  private int myMinimumAndPreferredWidth;
  protected boolean myPaintingNow;

  public ComboBox() {
    this(-1);
  }

  public ComboBox(final ComboBoxModel model) {
    this(model, -1);
  }

  /**
   * @param width preferred width of the combobox. Value <code>-1</code> means undefined.
   */
  public ComboBox(final int width) {
    this(new DefaultComboBoxModel(), width);
  }

  public ComboBox(final ComboBoxModel model, final int width) {
    super(model);
    myMinimumAndPreferredWidth = width;
    registerCancelOnEscape();
    UIUtil.installComboBoxCopyAction(this);
  }

  @Override
  public void setPopupVisible(boolean visible) {
    if (getModel().getSize() == 0 && visible) return;
    if (visible && JBPopupFactory.getInstance().getChildFocusedPopup(this) != null) return;

    final boolean wasShown = isPopupVisible();
    super.setPopupVisible(visible);
    if (!wasShown
        && visible
        && isEditable()
        && !UIManager.getBoolean("ComboBox.isEnterSelectablePopup")) {

      final ComboBoxEditor editor = getEditor();
      final Object item = editor.getItem();
      final Object selectedItem = getSelectedItem();
      if ((item == null || item != selectedItem)) {
        configureEditor(editor, selectedItem);
      }
    }
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    if (event.getID() == WindowEvent.WINDOW_OPENED) {
      final WindowEvent we = (WindowEvent)event;
      final List<JBPopup> popups = JBPopupFactory.getInstance().getChildPopups(this);
      if (popups != null) {
        for (JBPopup each : popups) {
          if (each.getContent() != null && SwingUtilities.isDescendingFrom(each.getContent(), we.getWindow())) {
            super.setPopupVisible(false);
          }
        }
      }
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();

    if (SwingUtilities.getAncestorOfClass(JTable.class, this) != null) {
      putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
    }

    Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    Toolkit.getDefaultToolkit().removeAWTEventListener(this);
  }


  @Nullable
  public ComboPopup getPopup() {
    ComboBoxUI ui = getUI();
    if (ui instanceof BasicComboBoxUI bui) {
      return BasicComboBoxUIHacking.getPopup(bui);
    }
    return null;
  }

  public ComboBox(final E[] items, final int preferredWidth) {
    super(items);
    myMinimumAndPreferredWidth = preferredWidth;
    registerCancelOnEscape();
  }

  public ComboBox(@Nonnull E[] items) {
    this(items, -1);
  }

  public void setMinimumAndPreferredWidth(final int minimumAndPreferredWidth) {
    myMinimumAndPreferredWidth = minimumAndPreferredWidth;
  }

  private void registerCancelOnEscape() {
    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final DialogWrapper dialogWrapper = DialogWrapper.findInstance(ComboBox.this);

        if (isPopupVisible()) {
          setPopupVisible(false);
        }
        else {
          //noinspection HardCodedStringLiteral
          final Object clientProperty = getClientProperty("tableCellEditor");
          if (clientProperty instanceof CellEditor) {
            // If combo box is inside editable table then we need to cancel editing
            // and do not close heavy weight dialog container (if any)
            ((CellEditor)clientProperty).cancelCellEditing();
          }
          else if (dialogWrapper != null) {
            dialogWrapper.doCancelAction();
          }
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
  }

  @Override
  public final void setEditor(final ComboBoxEditor editor) {
    ComboBoxEditor _editor = editor;
    if (SystemInfo.isMac && UIUtil.isUnderAquaLookAndFeel()) {
      if ("AquaComboBoxEditor".equals(editor.getClass().getSimpleName())) {
        _editor = new FixedComboBoxEditor();
      }
    }

    super.setEditor(new MyEditor(this, _editor));
  }

  @Override
  public final Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getPreferredSize() {
    int width = myMinimumAndPreferredWidth;
    final Dimension preferredSize = super.getPreferredSize();
    if (width < 0) {
      width = preferredSize.width;
    }

    return new Dimension(width, preferredSize.height);
  }

  @Override
  public boolean hasFocus() {
    if (SystemInfo.isMac && UIUtil.isUnderAquaLookAndFeel() && myPaintingNow) {
      return false;
    }
    return super.hasFocus();
  }

  @Override
  protected Dimension getOriginalPreferredSize() {
    return super.getPreferredSize();
  }

  @Override
  public void paint(Graphics g) {
    try {
      myPaintingNow = true;
      super.paint(g);
      if (Boolean.TRUE != getClientProperty("JComboBox.isTableCellEditor")) MacUIUtil.drawComboboxFocusRing(this, g);
    } finally {
      myPaintingNow = false;
    }
  }

  private static final class MyEditor implements ComboBoxEditor {
    private final JComboBox myComboBox;
    private final ComboBoxEditor myDelegate;

    public MyEditor(final JComboBox comboBox, final ComboBoxEditor delegate) {
      myComboBox = comboBox;
      myDelegate = delegate;
      if (myDelegate != null) {
        myDelegate.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (myComboBox.isPopupVisible()) {
              myComboBox.setPopupVisible(false);
            }
            else {
              //noinspection HardCodedStringLiteral
              final Object clientProperty = myComboBox.getClientProperty("tableCellEditor");
              if (clientProperty instanceof CellEditor) {
                // If combo box is inside editable table then we need to cancel editing
                // and do not close heavy weight dialog container (if any)
                ((CellEditor)clientProperty).stopCellEditing();
              }
              else {
                myComboBox.setSelectedItem(getItem());
                final JRootPane rootPane = myComboBox.getRootPane();
                if (rootPane != null) {
                  final JButton button = rootPane.getDefaultButton();
                  if (button != null) {
                    button.doClick();
                  }
                }
              }
            }
          }
        });
      }
    }

    @Override
    public void addActionListener(final ActionListener l) {
    }

    @Override
    public Component getEditorComponent() {
      if (myDelegate != null) {
        return myDelegate.getEditorComponent();
      }
      else {
        return null;
      }
    }

    @Override
    public Object getItem() {
      if (myDelegate != null) {
        return myDelegate.getItem();
      }
      else {
        return null;
      }
    }

    @Override
    public void removeActionListener(final ActionListener l) {
    }

    @Override
    public void selectAll() {
      if (myDelegate != null) {
        myDelegate.selectAll();
      }
    }

    @Override
    public void setItem(final Object obj) {
      if (myDelegate != null) {
        myDelegate.setItem(obj);
      }
    }
  }
}
