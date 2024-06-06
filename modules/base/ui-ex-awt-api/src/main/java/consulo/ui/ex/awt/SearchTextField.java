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
package consulo.ui.ex.awt;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.SystemInfo;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author max
 */
public class SearchTextField extends JPanel {
  public static final KeyStroke SHOW_HISTORY_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
  public static final CustomShortcutSet SHOW_HISTORY_SHORTCUT = new CustomShortcutSet(SHOW_HISTORY_KEYSTROKE);
  public static final KeyStroke ALT_SHOW_HISTORY_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK);
  public static final CustomShortcutSet ALT_SHOW_HISTORY_SHORTCUT = new CustomShortcutSet(ALT_SHOW_HISTORY_KEYSTROKE);

  private int myHistorySize = 5;
  private final MyModel myModel;
  private final TextFieldWithProcessing myTextField;

  private JBPopup myPopup;
  private JLabel myClearFieldLabel;
  private JLabel myToggleHistoryLabel;
  private JPopupMenu myNativeSearchPopup;
  private JMenuItem myNoItems;

  public SearchTextField() {
    this(true);
  }

  public SearchTextField(boolean historyEnabled) {
    super(new BorderLayout());

    myModel = new MyModel();

    myTextField = new TextFieldWithProcessing() {
      @Override
      public void processKeyEvent(final KeyEvent e) {
        if (preprocessEventForTextField(e)) return;
        super.processKeyEvent(e);
      }

      @Override
      public void setBackground(final Color bg) {
        super.setBackground(bg);
        if (!hasIconsOutsideOfTextField()) {
          if (myClearFieldLabel != null) {
            myClearFieldLabel.setBackground(bg);
          }
        }
        if (myToggleHistoryLabel != null) {
          myToggleHistoryLabel.setBackground(bg);
        }
      }
    };
    myTextField.setColumns(15);
    myTextField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        onFocusLost();
        super.focusLost(e);
      }

      @Override
      public void focusGained(FocusEvent e) {
        onFocusGained();
        super.focusGained(e);
      }
    });
    add(myTextField, BorderLayout.CENTER);
    myTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
          if (isSearchControlUISupported() && myNativeSearchPopup != null) {
            myNativeSearchPopup.show(myTextField, 5, myTextField.getHeight());
          }
          else if (myPopup == null || !myPopup.isVisible()) {
            showPopup();
          }
        }
      }
    });

    if (isSearchControlUISupported()) {
      myTextField.putClientProperty("JTextField.variant", "search");
      myTextField.putClientProperty("JTextField.Search.Gap", JBUIScale.scale(4));
      myTextField.putClientProperty("JTextField.Search.CancelAction", new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myTextField.setText("");
          onFieldCleared();
        }
      });

      if (historyEnabled) {
        myNativeSearchPopup = new JBPopupMenu();
        myNoItems = new JBMenuItem("No recent searches");
        myNoItems.setEnabled(false);

        updateMenu();
        myTextField.putClientProperty("JTextField.Search.FindPopup", myNativeSearchPopup);
      }
    }
    else {
      myToggleHistoryLabel = new JLabel(TargetAWT.to(AllIcons.Actions.SearchWithHistory));
      myToggleHistoryLabel.setOpaque(true);
      myToggleHistoryLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          togglePopup();
        }
      });
      if (historyEnabled) {
        add(myToggleHistoryLabel, BorderLayout.WEST);
      }

      myClearFieldLabel = new JLabel(TargetAWT.to(PlatformIconGroup.actionsClose()));
      myClearFieldLabel.setOpaque(true);
      add(myClearFieldLabel, BorderLayout.EAST);
      myClearFieldLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          myTextField.setText("");
          onFieldCleared();
        }
      });

      if (!hasIconsOutsideOfTextField()) {
        final Border originalBorder;
        if (Platform.current().os().isMac()) {
          originalBorder = BorderFactory.createLoweredBevelBorder();
        }
        else {
          originalBorder = myTextField.getBorder();
        }

        myToggleHistoryLabel.setBackground(myTextField.getBackground());
        myClearFieldLabel.setBackground(myTextField.getBackground());

        setBorder(new CompoundBorder(IdeBorderFactory.createEmptyBorder(2, 0, 2, 0), originalBorder));

        myTextField.setOpaque(true);
        myTextField.setBorder(IdeBorderFactory.createEmptyBorder(0, 5, 0, 5));
      }
      else {
        setBorder(IdeBorderFactory.createEmptyBorder(2, 0, 2, 0));
      }
    }

    if (toClearTextOnEscape()) {
      final ActionManager actionManager = ActionManager.getInstance();
      if (actionManager != null) {
        EmptyAction.registerWithShortcutSet(IdeActions.ACTION_CLEAR_TEXT, CommonShortcuts.ESCAPE, this);
      }
    }
  }

  protected boolean toClearTextOnEscape() {
    return ApplicationManager.getApplication() != null;
  }

  protected void onFieldCleared() {
  }

  protected void onFocusLost() {
  }

  protected void onFocusGained() {
  }

  private void updateMenu() {
    if (myNativeSearchPopup != null) {
      myNativeSearchPopup.removeAll();
      final int itemsCount = myModel.getSize();
      if (itemsCount == 0) {
        myNativeSearchPopup.add(myNoItems);
      }
      else {
        for (int i = 0; i < itemsCount; i++) {
          final String item = myModel.getElementAt(i);
          addMenuItem(item);
        }
      }
    }
  }

  protected boolean isSearchControlUISupported() {
    return (SystemInfo.isMacOSLeopard && UIUtil.isUnderAquaLookAndFeel()) || UIUtil.isUnderDarcula() || UIUtil.isUnderIntelliJLaF();
  }

  protected boolean hasIconsOutsideOfTextField() {
    return UIUtil.isUnderGTKLookAndFeel() || UIUtil.isUnderNimbusLookAndFeel();
  }

  public void addDocumentListener(DocumentListener listener) {
    getTextEditor().getDocument().addDocumentListener(listener);
  }

  public void removeDocumentListener(DocumentListener listener) {
    getTextEditor().getDocument().removeDocumentListener(listener);
  }

  public void addKeyboardListener(final KeyListener listener) {
    getTextEditor().addKeyListener(listener);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (myToggleHistoryLabel != null) {
      final Color bg = enabled ? UIUtil.getTextFieldBackground() : UIUtil.getPanelBackground();
      myToggleHistoryLabel.setBackground(bg);
      myClearFieldLabel.setBackground(bg);
    }
  }

  public void setHistorySize(int historySize) {
    if (historySize <= 0) throw new IllegalArgumentException("history size must be a positive number");
    myHistorySize = historySize;
  }

  public void setHistory(List<String> aHistory) {
    myModel.setItems(aHistory);
  }

  public List<String> getHistory() {
    final int itemsCount = myModel.getSize();
    final List<String> history = new ArrayList<>(itemsCount);
    for (int i = 0; i < itemsCount; i++) {
      history.add(myModel.getElementAt(i));
    }
    return history;
  }

  public void setText(String aText) {
    getTextEditor().setText(aText);
  }

  public String getText() {
    return getTextEditor().getText();
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    hidePopup();
  }

  public void addCurrentTextToHistory() {
    if ((myNativeSearchPopup != null && myNativeSearchPopup.isVisible()) || (myPopup != null && myPopup.isVisible())) {
      return;
    }
    final String item = getText();
    myModel.addElement(item);
  }

  private void addMenuItem(final String item) {
    if (myNativeSearchPopup != null) {
      myNativeSearchPopup.remove(myNoItems);
      final JMenuItem menuItem = new JBMenuItem(item);
      myNativeSearchPopup.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          myTextField.setText(item);
          addCurrentTextToHistory();
        }
      });
    }
  }

  public void selectText() {
    getTextEditor().selectAll();
  }

  public JBTextField getTextEditor() {
    return myTextField;
  }

  @Override
  public boolean requestFocusInWindow() {
    return myTextField.requestFocusInWindow();
  }

  @Override
  public void requestFocus() {
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(getTextEditor());
  }

  public class MyModel extends AbstractListModel {
    private List<String> myFullList = new ArrayList<>();

    private String mySelectedItem;

    @Override
    public String getElementAt(int index) {
      return myFullList.get(index);
    }

    @Override
    public int getSize() {
      return Math.min(myHistorySize, myFullList.size());
    }

    public void addElement(String item) {
      final String newItem = item.trim();
      if (newItem.isEmpty()) {
        return;
      }

      final int length = myFullList.size();
      int index = -1;
      for (int i = 0; i < length; i++) {
        if (StringUtil.equalsIgnoreCase(myFullList.get(i), newItem)) {
          index = i;
          break;
        }
      }
      if (index == 0) {
        // item is already at the top of the list
        return;
      }
      else if (index > 0) {
        // move item to top of the list
        myFullList.remove(index);
      }
      else if (myFullList.size() >= myHistorySize && myFullList.size() > 0) {
        // trim list
        myFullList.remove(myFullList.size() - 1);
      }
      insertElementAt(newItem, 0);
    }

    public void insertElementAt(String item, int index) {
      myFullList.add(index, item);
      fireContentsChanged();
    }

    public String getSelectedItem() {
      return mySelectedItem;
    }

    public void setSelectedItem(String anItem) {
      mySelectedItem = anItem;
    }

    public void fireContentsChanged() {
      fireContentsChanged(this, -1, -1);
      updateMenu();
    }

    public void setItems(List<String> aList) {
      myFullList = new ArrayList<>(aList);
      fireContentsChanged();
    }
  }

  private void hidePopup() {
    if (myPopup != null) {
      myPopup.cancel();
      myPopup = null;
    }
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    Border border = super.getBorder();
    if (border != null && UIUtil.isUnderAquaLookAndFeel()) {
      JBInsets.addTo(size, border.getBorderInsets(this));
    }
    return size;
  }

  protected Consumer<String> createItemChosenCallback() {
    return (value) -> {
      getTextEditor().setText(value != null ? value : "");
      addCurrentTextToHistory();
      if (myPopup != null) {
        myPopup.cancel();
        myPopup = null;
      }
    };
  }

  protected void showPopup() {
    if (myPopup == null || !myPopup.isVisible()) {
      Consumer<String> itemChosenCallback = createItemChosenCallback();
      myPopup = JBPopupFactory.getInstance().<String>createPopupChooserBuilder(myModel.myFullList).setMovable(false).setRequestFocus(true).setItemChosenCallback(itemChosenCallback).createPopup();
      if (isShowing()) {
        myPopup.showUnderneathOf(getPopupLocationComponent());
      }
    }
  }

  protected Component getPopupLocationComponent() {
    return hasIconsOutsideOfTextField() ? myToggleHistoryLabel : this;
  }

  private void togglePopup() {
    if (myPopup == null) {
      showPopup();
    }
    else {
      hidePopup();
    }
  }

  public void setSelectedItem(final String s) {
    getTextEditor().setText(s);
  }

  public int getSelectedIndex() {
    return myModel.myFullList.indexOf(getText());
  }

  protected static class TextFieldWithProcessing extends JBTextField {
    @Override
    public void processKeyEvent(KeyEvent e) {
      super.processKeyEvent(e);
    }
  }

  public final void keyEventToTextField(KeyEvent e) {
    myTextField.processKeyEvent(e);
  }

  protected boolean preprocessEventForTextField(KeyEvent e) {
    return false;
  }

  public void setSearchIcon(final Image icon) {
    if (!isSearchControlUISupported()) {
      myToggleHistoryLabel.setIcon(TargetAWT.to(icon));
    }
  }
}
