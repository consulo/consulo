/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.LightColors;
import com.intellij.util.ui.CenteredIcon;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import org.mustbe.consulo.RequiredDispatchThread;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CardActionsPanel extends JPanel {
  private final JBCardLayout myLayout = new JBCardLayout();
  private final JPanel myContent = new JPanel(myLayout);
  private int nCards = 0;

  public CardActionsPanel(ActionGroup rootGroup) {
    setLayout(new GridLayout(1, 1));
    add(myContent);
    createCardForGroup(rootGroup, "root", null);
  }

  private void createCardForGroup(ActionGroup group, String cardId, final String parentId) {
    JPanel card = new JPanel(new BorderLayout());

    JPanel withBottomFiller = new JPanel(new BorderLayout());
    withBottomFiller.add(card, BorderLayout.NORTH);
    myContent.add(withBottomFiller, cardId);

    List<Button> buttons = buildButtons(group, cardId);
    if(parentId != null)
    {
      AnAction back = new AnAction(null, null, null) {
        @RequiredDispatchThread
        @Override
        public void actionPerformed(AnActionEvent e) {
          myLayout.swipe(myContent, parentId, JBCardLayout.SwipeDirection.BACKWARD);
        }
      };

      Presentation p = new Presentation("Back");
      p.setIcon(AllIcons.Actions.Back);
      buttons.add(0, new Button(back, p));
    }

    JPanel buttonsPanel = new JPanel(new GridLayout(buttons.size(), 1, JBUI.scale(5), JBUI.scale(5)));
    buttonsPanel.setBorder(JBUI.Borders.empty(15, 15, 15, 15));
    for (Button button : buttons) {
      buttonsPanel.add(button);
    }
    card.add(buttonsPanel, BorderLayout.CENTER);
  }

  private List<Button> buildButtons(ActionGroup group, String parentId) {
    AnAction[] actions = group.getChildren(null);

    List<Button> buttons = new ArrayList<Button>();

    for (AnAction action : actions) {
      Presentation presentation = action.getTemplatePresentation();
      if (action instanceof ActionGroup) {
        ActionGroup childGroup = (ActionGroup)action;
        if (childGroup.isPopup()) {
          final String id = String.valueOf(++nCards);
          createCardForGroup(childGroup, id, parentId);

          buttons.add(new Button(new ActivateCard(id), presentation));
        }
        else {
          buttons.addAll(buildButtons(childGroup, parentId));
        }
      }
      else {
        action.update(new AnActionEvent(null, DataManager.getInstance().getDataContext(this),
                                        ActionPlaces.WELCOME_SCREEN, presentation, ActionManager.getInstance(), 0));
        if (presentation.isVisible()) {
          buttons.add(new Button(action, presentation));
        }
      }
    }
    return buttons;
  }

  private static class Button extends ActionButtonWithText {
    private static final Icon DEFAULT_ICON = new Icon() {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(LightColors.SLIGHTLY_GREEN);
        int scale4 = JBUI.scale(4);
        int scale8 = JBUI.scale(8);
        g.fillRoundRect(x + scale4, y + scale4, getIconWidth() - scale8, getIconHeight() - scale8, scale8, scale8);
        g.setColor(Color.GRAY);
        g.drawRoundRect(x + scale4, y + scale4, getIconWidth() - scale8, getIconHeight() - scale8, scale8, scale8);
      }

      @Override
      public int getIconWidth() {
        return JBUI.scale(32);
      }

      @Override
      public int getIconHeight() {
        return JBUI.scale(32);
      }
    };

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      AnAction action = getAction();
      if (action instanceof ActivateCard) {
        Rectangle bounds = getBounds();

        Icon icon = AllIcons.Actions.Forward; //AllIcons.Icons.Ide.NextStepGrayed;
        int y = (bounds.height - icon.getIconHeight()) / 2;
        int x = bounds.width - icon.getIconWidth() - JBUI.scale(15);

        if (getPopState() == POPPED) {
          final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
          g.setColor(WelcomeScreenColors.CAPTION_BACKGROUND);
          int scale3 = JBUI.scale(3);
          int scale6 = JBUI.scale(6);
          g.fillOval(x - scale3, y - scale3, icon.getIconWidth() + scale6, icon.getIconHeight() + scale6);

          g.setColor(WelcomeScreenColors.GROUP_ICON_BORDER_COLOR);
          g.drawOval(x - scale3, y - scale3, icon.getIconWidth() + scale6, icon.getIconHeight() + scale6);
          config.restore();
        }
        else {
          icon = IconLoader.getDisabledIcon(icon);
        }

        icon.paintIcon(this, g, x, y);
      }
    }

    public Button(AnAction action, Presentation presentation) {
      super(action,
            wrapIcon(presentation),
            ActionPlaces.WELCOME_SCREEN,
            JBUI.size(32, 32));
      setBorder(JBUI.Borders.empty(3, 3, 3, 3));
    }

    @Override
    public String getToolTipText() {
      return null;
    }

    @Override
    protected int horizontalTextAlignment() {
      return SwingConstants.LEFT;
    }

    @Override
    protected int iconTextSpace() {
      return JBUI.scale(8);
    }

    private static Presentation wrapIcon(Presentation presentation) {
      Icon original = presentation.getIcon();
      CenteredIcon centered = new CenteredIcon(original != null ? original : DEFAULT_ICON, JBUI.scale(40), JBUI.scale(40), false);
      presentation.setIcon(centered);
      return presentation;
    }
  }

  private class ActivateCard extends AnAction {
    private final String myId;

    public ActivateCard(String id) {
      myId = id;
    }

    @RequiredDispatchThread
    @Override
    public void actionPerformed(AnActionEvent e) {
      myLayout.swipe(myContent, myId, JBCardLayout.SwipeDirection.FORWARD);
    }
  }
}
