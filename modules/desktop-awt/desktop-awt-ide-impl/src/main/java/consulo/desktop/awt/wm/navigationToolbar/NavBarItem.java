// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.application.AllIcons;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.wm.navigationToolbar.ui.NavBarUI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.ui.ex.tree.TreeAnchorizerValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarItem extends SimpleColoredComponent implements UiDataProvider, Disposable {
  private final String myText;
  private final SimpleTextAttributes myAttributes;
  private final int myIndex;
  private final Image myIcon;
  private final NavBarPanel myPanel;
  private final TreeAnchorizerValue<?> myObject;
  private final boolean isPopupElement;
  private final NavBarUI myUI;
  private final boolean myNeedPaintIcon;

  public NavBarItem(NavBarPanel panel, Object object, int idx, Disposable parent) {
    this(panel, object, idx, parent, false);
  }

  public NavBarItem(NavBarPanel panel, Object object, int idx, Disposable parent, boolean inPopup) {
    myPanel = panel;
    myUI = panel.getNavBarUI();
    myObject = object == null ? null : TreeAnchorizer.getService().createAnchorValue(object);
    myIndex = idx;
    isPopupElement = idx == -1;
    myNeedPaintIcon = false;

    if (object != null) {
      NavBarPresentation presentation = myPanel.getPresentation();
      myText = presentation.getPresentableText(object, inPopup);
      myIcon = presentation.getIcon(object);
      myAttributes = presentation.getTextAttributes(object, false);
    }
    else {
      myText = "Sample";
      myIcon = AllIcons.Nodes.Folder;
      myAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    Disposer.register(parent == null ? panel : parent, this);

    setOpaque(false);
    setIpad(myUI.getElementIpad(isPopupElement));

    if (!isPopupElement) {
      setBorder(null);
      setPaintFocusBorder(false);
      setIconOpaque(false);
      if (myPanel.allowNavItemsFocus()) {
        // Take ownership of Tab/Shift-Tab navigation (to move focus out of nav bar panel), as
        // navigation between items is handled by the Left/Right cursor keys. This is similar
        // to the behavior a JRadioButton contained inside a GroupBox.
        setFocusTraversalKeysEnabled(false);
        setFocusable(true);
        addKeyListener(new KeyHandler());
        addFocusListener(new FocusHandler());
      }
    }
    else {
      setIconOpaque(true);
      setFocusBorderAroundIcon(true);
    }

    update();
  }

  /**
   * Constructor accepting pre-computed presentation data.
   * No ReadAction needed — all presentation is pre-computed on background thread.
   */
  public NavBarItem(NavBarPanel panel, NavBarItemData data, int idx, Disposable parent) {
    myPanel = panel;
    myUI = panel.getNavBarUI();
    myObject = TreeAnchorizer.getService().tryCreateAnchorValue(data.element());
    myIndex = idx;
    isPopupElement = false;
    myText = data.text();
    myIcon = data.icon();
    myAttributes = data.attributes();
    myNeedPaintIcon = data.needPaintIcon();

    Disposer.register(parent == null ? panel : parent, this);

    setOpaque(false);
    setIpad(myUI.getElementIpad(false));
    setBorder(null);
    setPaintFocusBorder(false);
    setIconOpaque(false);
    if (myPanel.allowNavItemsFocus()) {
      setFocusTraversalKeysEnabled(false);
      setFocusable(true);
      addKeyListener(new KeyHandler());
      addFocusListener(new FocusHandler());
    }

    update();
  }

  public NavBarItem(NavBarPanel panel, Object object, Disposable parent, boolean inPopup) {
    this(panel, object, -1, parent, inPopup);
  }

  public Object getObject() {
    return myObject == null ? null : myObject.extractValue();
  }

  public SimpleTextAttributes getAttributes() {
    return myAttributes;
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  @Override
  public Font getFont() {
    return myUI == null ? super.getFont() : myUI.getElementFont(this);
  }

  void update() {
    clear();

    setIcon(myIcon);

    boolean focused = isFocusedOrPopupElement();
    boolean selected = isSelected();

    setBackground(myUI.getBackground(selected, focused));

    Color fg = myUI.getForeground(selected, focused, isInactive());
    if (fg == null) fg = myAttributes.getFgColor();

    Color bg = getBackground();
    append(myText, new SimpleTextAttributes(bg, fg, myAttributes.getWaveColor(), myAttributes.getStyle()));

    //repaint();
  }

  public boolean isInactive() {
    NavBarModel model = myPanel.getModel();
    return model.getSelectedIndex() < myIndex && model.getSelectedIndex() != -1 && !myPanel.isUpdating();
  }

  public boolean isPopupElement() {
    return isPopupElement;
  }

  @Override
  protected void doPaint(Graphics2D g) {
    if (isPopupElement) {
      super.doPaint(g);
    }
    else {
      myUI.doPaintNavBarItem(g, this, myPanel);
    }
  }

  public int doPaintText(Graphics2D g, int offset) {
    return super.doPaintText(g, offset, false);
  }

  public boolean isLastElement() {
    return myIndex == myPanel.getModel().size() - 1;
  }

  public boolean isFirstElement() {
    return myIndex == 0;
  }

  @Override
  public void setOpaque(boolean isOpaque) {
    super.setOpaque(false);
  }

  @Nonnull
  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    Dimension offsets = myUI.getOffsets(this);
    int width = size.width + offsets.width;
    if (!needPaintIcon() && myIcon != null) {
      width -= myIcon.getWidth();
    }
    return new Dimension(width, size.height + offsets.height);
  }

  public boolean needPaintIcon() {
    return myNeedPaintIcon;
  }

  @Nonnull
  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  private boolean isFocusedOrPopupElement() {
    return isFocused() || isPopupElement;
  }

  public boolean isFocused() {
    if (myPanel.allowNavItemsFocus()) {
      return UIUtil.isFocusAncestor(myPanel) && !myPanel.isNodePopupActive();
    }
    else {
      return myPanel.hasFocus() && !myPanel.isNodePopupActive();
    }
  }

  public boolean isSelected() {
    NavBarModel model = myPanel.getModel();
    return isPopupElement ? myPanel.isSelectedInPopup(getObject()) : model.getSelectedIndex() == myIndex;
  }

  @Override
  protected boolean shouldDrawBackground() {
    return isSelected() && isFocusedOrPopupElement();
  }

  @Override
  public void dispose() {
  }

  public boolean isNextSelected() {
    return myIndex == myPanel.getModel().getSelectedIndex() - 1;
  }

  @Override
  public void uiDataSnapshot(@Nonnull DataSink sink) {
    Object obj = getObject();
    sink.set(NavBarPanel.NAV_BAR_ITEMS, obj != null ? List.of(obj) : List.of());
    // Delegate non-selection data to panel (Project, CopyPaste, IdeView, etc.)
    sink.uiDataSnapshot(myPanel);
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleNavBarItem();
    }
    return accessibleContext;
  }

  protected class AccessibleNavBarItem extends AccessibleSimpleColoredComponent implements AccessibleAction {
    @Override
    public AccessibleRole getAccessibleRole() {
      if (!isPopupElement()) {
        return AccessibleRole.PUSH_BUTTON;
      }
      return super.getAccessibleRole();
    }

    @Override
    public AccessibleAction getAccessibleAction() {
      return this;
    }

    @Override
    public int getAccessibleActionCount() {
      return !isPopupElement() ? 1 : 0;
    }

    @Override
    public String getAccessibleActionDescription(int i) {
      if (i == 0 && !isPopupElement()) {
        return UIManager.getString("AbstractButton.clickText");
      }
      return null;
    }

    @Override
    public boolean doAccessibleAction(int i) {
      if (i == 0 && !isPopupElement()) {
        myPanel.getModel().setSelectedIndex(myIndex);
      }
      return false;
    }
  }

  private class KeyHandler extends KeyAdapter {
    // This listener checks if the key event is a KeyEvent.VK_TAB
    // or shift + KeyEvent.VK_TAB event, consume the event
    // if so and move the focus to next/previous component after/before
    // the containing NavBarPanel.
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_TAB) {
        // Check source is a nav bar item
        if (e.getSource() instanceof NavBarItem) {
          e.consume();
          jumpToNextComponent(!e.isShiftDown());
        }
      }
    }

    void jumpToNextComponent(boolean next) {
      // The base will be first or last NavBarItem in the NavBarPanel
      NavBarItem focusBase = null;
      List<NavBarItem> items = myPanel.getItems();
      if (items.size() > 0) {
        if (next) {
          focusBase = items.get(items.size() - 1);
        }
        else {
          focusBase = items.get(0);
        }
      }

      // Transfer focus
      if (focusBase != null) {
        if (next) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(focusBase);
        }
        else {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(focusBase);
        }
      }
    }
  }

  private class FocusHandler implements FocusListener {
    @Override
    public void focusGained(FocusEvent e) {
      myPanel.fireNavBarItemFocusGained(e);
    }

    @Override
    public void focusLost(FocusEvent e) {
      myPanel.fireNavBarItemFocusLost(e);
    }
  }
}
