/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.welcomeScreen;

import consulo.dataContext.DataManager;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.WelcomePopupAction;
import consulo.ide.impl.idea.ui.JBCardLayout;
import consulo.ide.impl.idea.ui.components.labels.ActionLink;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.JBUI;
import consulo.disposer.Disposable;
import consulo.ide.impl.welcomeScreen.WelcomeScreenConstants;
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlideAction;
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlider;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public class FlatWelcomeScreen extends JPanel implements WelcomeScreenSlider {
  public static final String MAIN = "main";

  private final FlatWelcomePanel myMainWelcomePanel;
  private final FlatWelcomeFrame myWelcomeFrame;

  @RequiredUIAccess
  public FlatWelcomeScreen(FlatWelcomeFrame welcomeFrame) {
    super(new JBCardLayout());
    myWelcomeFrame = welcomeFrame;
    myMainWelcomePanel = new FlatWelcomePanel(welcomeFrame) {
      @Override
      @RequiredUIAccess
      public JComponent createActionPanel() {
        return FlatWelcomeScreen.this.createActionPanel(this);
      }
    };
    add(myMainWelcomePanel, MAIN);

    registerKeyboardAction(e -> {
      for (Component component : getComponents()) {
        if (component.isVisible() && component != myMainWelcomePanel) {
          removeSlide((JComponent)component);
          break;
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  public FlatWelcomePanel getMainWelcomePanel() {
    return myMainWelcomePanel;
  }

  @RequiredUIAccess
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
          @RequiredUIAccess
          @Override
          public void actionPerformed(@Nonnull AnActionEvent e) {
            JComponent panel = oldAction.createSlide(myWelcomeFrame, FlatWelcomeScreen.this);
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
        ActionLink link = new ActionLink(text, presentation.getIcon(), action, createUsageTracker(action));
        // Don't allow focus, as the containing panel is going to focusable.
        link.setFocusable(false);
        link.setPaintUnderline(false);
        link.setNormalColor(WelcomeScreenConstants.getLinkNormalColor());
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

  public static void collectAllActions(List<AnAction> group, ActionGroup actionGroup) {
    for (AnAction action : actionGroup.getChildren(null)) {
      if (action instanceof ActionGroup && !((ActionGroup)action).isPopup()) {
        collectAllActions(group, (ActionGroup)action);
      }
      else {
        group.add(action);
      }
    }
  }

  @Override
  public void setTitle(@Nonnull String title) {
    myWelcomeFrame.setTitle(title);
  }

  @Override
  public void removeSlide(@Nonnull JComponent target) {
    JBCardLayout layout = (JBCardLayout)getLayout();

    layout.swipe(this, MAIN, JBCardLayout.SwipeDirection.BACKWARD, () -> remove(target));

    if(target instanceof Disposable) {
      ((Disposable)target).dispose();
    }

    myWelcomeFrame.setDefaultTitle();
  }

  private static Runnable createUsageTracker(final AnAction action) {
    return () -> UsageTrigger.trigger("welcome.screen." + ActionManager.getInstance().getId(action));
  }
}
