package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.ui.ClickListener;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 15:52/30.09.13
 */
public class WelcomePaneMain extends JPanel {
  private class HeaderButton extends JPanel {
    private final String myKey;
    private boolean myActive;

    public HeaderButton(String text, String key) {
      myKey = key;
      setBorder(new BottomLineBorder());

      JLabel titleLabel = new JLabel(text);
      titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
      titleLabel.setForeground(WelcomeScreenColors.CAPTION_FOREGROUND);
      add(titleLabel);

      new ClickListener() {
        @Override
        public boolean onClick(MouseEvent event, int clickCount) {
          setActive(true);
          return true;
        }
      }.installOn(this);
    }

    public void setActive(boolean active) {
      myActive = active;

      if(myActive) {
        setBackground(UIUtil.getMenuItemSelectedBackground());

        for (HeaderButton o : myButtons) {
          if(o == this) {
            continue;
          }

          o.setActive(false);
        }

        ((CardLayout)myCardPanel.getLayout()).show(myCardPanel, myKey);
      }
      else {
        setBackground(WelcomeScreenColors.CAPTION_BACKGROUND);
      }
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(super.getPreferredSize().width / 2, 28);
    }

    @Override
    public Dimension getMinimumSize() {
      return getMaximumSize();
    }

    @Override
    public Dimension getPreferredSize() {
      return getMaximumSize();
    }
  }

  private final JPanel myCardPanel;
  private final HeaderButton[] myButtons = new HeaderButton[2];

  public WelcomePaneMain(NewWelcomeScreen2.WelcomeScreenGroup root) {
    super(new GridBagLayout());

    JPanel headerPanel = new JPanel(new GridBagLayout());

    myButtons[0] = createTitle(headerPanel, "Recent Projects", 0);
    myButtons[1] = createTitle(headerPanel, "Actions", 1);

    myCardPanel = new JPanel(new CardLayout()) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(getWidth(), 500);
      }

    };

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0.33;
    c.weighty = 0;
    c.fill = GridBagConstraints.BOTH;
    add(headerPanel, c);

    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 0.33;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    add(myCardPanel, c);

    myCardPanel.add(new RecentProjectPanel(), "recent-projects");
    myCardPanel.add(new CardActionsPanel(root), "quick-start");

    myButtons[0].setActive(true);
  }

  public RecentProjectPanel getRecentProjectPanel() {
    return (RecentProjectPanel)myCardPanel.getComponent(0);
  }

  private HeaderButton createTitle(JPanel panel, String text, int val) {
    HeaderButton headerButton = new HeaderButton(text, val == 0 ? "recent-projects" : "quick-start");

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = val;
    c.gridy = 0;
    c.weightx = 0.33;
    c.fill = GridBagConstraints.BOTH;
    panel.add(headerButton, c);
    return headerButton;
  }
}
