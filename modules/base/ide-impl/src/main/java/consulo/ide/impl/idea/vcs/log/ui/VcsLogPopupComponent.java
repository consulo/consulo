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
package consulo.ide.impl.idea.vcs.log.ui;

import consulo.dataContext.DataManager;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.RoundedLineBorder;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.*;

public abstract class VcsLogPopupComponent extends JPanel {
  private static final int GAP_BEFORE_ARROW = 3;
  private static final int BORDER_SIZE = 2;

  @Nonnull
  private final String myName;
  @Nonnull
  private JLabel myNameLabel;
  @Nonnull
  private JLabel myValueLabel;

  protected VcsLogPopupComponent(@Nonnull String name) {
    myName = name;
  }

  public JComponent initUi() {
    myNameLabel = new JLabel(myName + ": ");
    myValueLabel = new JLabel() {
      @Override
      public String getText() {
        return getCurrentText();
      }
    };
    setDefaultForeground();
    setFocusable(true);
    setBorder(createUnfocusedBorder());

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(myNameLabel);
    add(myValueLabel);
    add(Box.createHorizontalStrut(GAP_BEFORE_ARROW));
    add(new JLabel(UIManager.getIcon("Tree.expandedIcon")));

    installChangeListener(() -> {
      myValueLabel.revalidate();
      myValueLabel.repaint();
    });
    showPopupMenuOnClick();
    showPopupMenuFromKeyboard();
    indicateHovering();
    indicateFocusing();
    return this;
  }


  public abstract String getCurrentText();

  public abstract void installChangeListener(@Nonnull Runnable onChange);

  /**
   * Create popup actions available under this filter.
   */
  protected abstract ActionGroup createActionGroup();

  private void indicateFocusing() {
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(@Nonnull FocusEvent e) {
        setBorder(createFocusedBorder());
      }

      @Override
      public void focusLost(@Nonnull FocusEvent e) {
        setBorder(createUnfocusedBorder());
      }
    });
  }

  private void showPopupMenuFromKeyboard() {
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(@Nonnull KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
          showPopupMenu();
        }
      }
    });
  }

  private void showPopupMenuOnClick() {
    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
        showPopupMenu();
        return true;
      }
    }.installOn(this);
  }

  private void indicateHovering() {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(@Nonnull MouseEvent e) {
        setOnHoverForeground();
      }

      @Override
      public void mouseExited(@Nonnull MouseEvent e) {
        setDefaultForeground();
      }
    });
  }

  private void setDefaultForeground() {
    boolean isDark = StyleManager.get().getCurrentStyle().isDark();
    myNameLabel.setForeground(isDark ? UIUtil.getLabelForeground() : UIUtil.getInactiveTextColor());
    myValueLabel.setForeground(isDark ? UIUtil.getLabelForeground() : UIUtil.getInactiveTextColor().darker().darker());
  }

  private void setOnHoverForeground() {
    boolean isDark = StyleManager.get().getCurrentStyle().isDark();
    myNameLabel.setForeground(isDark ? UIUtil.getLabelForeground() : UIUtil.getTextAreaForeground());
    myValueLabel.setForeground(isDark ? UIUtil.getLabelForeground() : UIUtil.getTextFieldForeground());
  }

  private void showPopupMenu() {
    ListPopup popup = createPopupMenu();
    popup.showUnderneathOf(this);
  }

  @Nonnull
  protected ListPopup createPopupMenu() {
    return JBPopupFactory.getInstance().
      createActionGroupPopup(null, createActionGroup(), DataManager.getInstance().getDataContext(this),
                             JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
  }

  private static Border createFocusedBorder() {
    return BorderFactory.createCompoundBorder(new RoundedLineBorder(UIUtil.getHeaderActiveColor(), 10, BORDER_SIZE), JBUI.Borders.empty(2));
  }

  private static Border createUnfocusedBorder() {
    return BorderFactory
      .createCompoundBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE), JBUI.Borders.empty(2));
  }
}
