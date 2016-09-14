/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ide.welcomeScreen;

import com.intellij.ide.DataManager;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.wm.WelcomeScreen;
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame;
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomePanel;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomePopupAction;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public class FlatWelcomeScreen extends JPanel implements WelcomeScreen {
  public static final String MAIN = "main";

  private final FlatWelcomePanel myMainWelcomePanel;
  private final FlatWelcomeFrame myWelcomeFrame;

  public FlatWelcomeScreen(FlatWelcomeFrame welcomeFrame) {
    super(new JBCardLayout());
    myWelcomeFrame = welcomeFrame;

    myMainWelcomePanel = new FlatWelcomePanel(welcomeFrame) {
      @Override
      public JComponent createActionPanel() {
        return FlatWelcomeScreen.this.createActionPanel(this);
      }
    };
    add(myMainWelcomePanel, MAIN);
  }

  public FlatWelcomePanel getMainWelcomePanel() {
    return myMainWelcomePanel;
  }

  private JComponent createActionPanel(FlatWelcomePanel welcomePanel) {
    JPanel actions = new NonOpaquePanel();
    actions.setBorder(JBUI.Borders.emptyLeft(10));
    actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup quickStart = (ActionGroup)actionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
    List<AnAction> group = new ArrayList<>();
    collectAllActions(group, quickStart);

    for (AnAction action : group) {
      AnActionEvent e = AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, DataManager.getInstance().getDataContext(welcomePanel));
      action.update(e);

      if (action instanceof WelcomeScreenSlideAction) {
        final WelcomeScreenSlideAction oldAction = (WelcomeScreenSlideAction)action;
        action = new AnAction() {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            JComponent panel = oldAction.createSlide(myWelcomeFrame, myWelcomeFrame::setTitle);
            JBCardLayout layout = (JBCardLayout)FlatWelcomeScreen.this.getLayout();
            String id = oldAction.getClass().getName();

            FlatWelcomeScreen.this.add(panel, id);

            layout.swipe(FlatWelcomeScreen.this, id, JBCardLayout.SwipeDirection.FORWARD);
          }
        };
        action.copyFrom(oldAction);
      }

      Presentation presentation = e.getPresentation();
      if (presentation.isVisible()) {
        String text = presentation.getText();
        if (text != null && text.endsWith("...")) {
          text = text.substring(0, text.length() - 3);
        }
        Icon icon = presentation.getIcon();
        if (icon.getIconHeight() != JBUI.scale(16) || icon.getIconWidth() != JBUI.scale(16)) {
          icon = JBUI.emptyIcon(16);
        }
        ActionLink link = new ActionLink(text, icon, action, createUsageTracker(action));
        // Don't allow focus, as the containing panel is going to focusable.
        link.setFocusable(false);
        link.setPaintUnderline(false);
        link.setNormalColor(FlatWelcomeFrame.getLinkNormalColor());
        FlatWelcomePanel.JActionLinkPanel button = new FlatWelcomePanel.JActionLinkPanel(link);
        button.setBorder(JBUI.Borders.empty(8, 20));
        if (action instanceof WelcomePopupAction) {
          button.add(FlatWelcomePanel.createArrow(link), BorderLayout.EAST);
        }
        welcomePanel.installFocusable(button, action, KeyEvent.VK_UP, KeyEvent.VK_DOWN, true);
        actions.add(button);
      }
    }

    JPanel panel = new JPanel();
    //panel.setBackground(FlatWelcomeFrame.getMainBackground());
    panel.add(actions);
    return panel;
  }

  private void collectAllActions(List<AnAction> group, ActionGroup actionGroup) {
    for (AnAction action : actionGroup.getChildren(null)) {
      if (action instanceof ActionGroup && !((ActionGroup)action).isPopup()) {
        collectAllActions(group, (ActionGroup)action);
      }
      else {
        group.add(action);
      }
    }
  }

  private static Runnable createUsageTracker(final AnAction action) {
    return () -> UsageTrigger.trigger("welcome.screen." + ActionManager.getInstance().getId(action));
  }

  @Override
  public JComponent getWelcomePanel() {
    return this;
  }

  @Override
  public void setupFrame(JFrame frame) {

  }

  @Override
  public void dispose() {

  }

  public void replacePanel(JComponent oldCard) {
    JBCardLayout layout = (JBCardLayout)getLayout();

    layout.swipe(this, MAIN, JBCardLayout.SwipeDirection.BACKWARD, () -> remove(oldCard));

    myWelcomeFrame.setDefaultTitle();
  }
}
