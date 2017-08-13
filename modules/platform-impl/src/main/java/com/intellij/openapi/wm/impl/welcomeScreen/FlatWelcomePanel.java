/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.IdeNotificationArea;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ClickListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.ParameterizedRunnable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.MouseEventAdapter;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextDelegate;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.welcomeScreen.BaseWelcomeScreenPanel;
import consulo.spash.AnimatedLogoLabel;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public abstract class FlatWelcomePanel extends BaseWelcomeScreenPanel<Void> {
  private FlatWelcomeFrame myFlatWelcomeFrame;
  public ParameterizedRunnable<List<NotificationType>> myEventListener;
  public Computable<Point> myEventLocation;

  @RequiredDispatchThread
  public FlatWelcomePanel(FlatWelcomeFrame flatWelcomeFrame) {
    super(flatWelcomeFrame, null);
    myFlatWelcomeFrame = flatWelcomeFrame;

    final JList projectsList = UIUtil.findComponentOfType(myLeftComponent, JList.class);
    if (projectsList != null) {
      projectsList.addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
          projectsList.repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
          projectsList.repaint();
        }
      });
    }
  }

  @RequiredDispatchThread
  public abstract JComponent createActionPanel();

  @NotNull
  @Override
  protected JComponent createLeftComponent(@NotNull Disposable parentDisposable, Void param) {
    return new NewRecentProjectPanel(parentDisposable);
  }

  @RequiredDispatchThread
  @Override
  @NotNull
  protected JComponent createRightComponent(Void param) {
    JPanel panel = new JPanel(new BorderLayout());
    JPanel logoPanel = new JPanel(new BorderLayout());
    logoPanel.setBorder(JBUI.Borders.empty(53, 66, 45, 0));
    AnimatedLogoLabel animatedLogoLabel = new AnimatedLogoLabel(8, false, true);
    animatedLogoLabel.setForeground(SandboxUtil.isInsideSandbox() ? JBColor.WHITE : JBColor.GRAY);
    logoPanel.add(animatedLogoLabel, BorderLayout.CENTER);

    panel.add(logoPanel, BorderLayout.NORTH);
    panel.add(createActionPanel(), BorderLayout.CENTER);
    panel.add(createSettingsAndDocs(), BorderLayout.SOUTH);
    return panel;
  }

  private JComponent createSettingsAndDocs() {
    JPanel panel = new NonOpaquePanel(new BorderLayout());
    NonOpaquePanel toolbar = new NonOpaquePanel();

    toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
    toolbar.add(createEventsLink());
    toolbar.add(createActionLink("Configure", IdeActions.GROUP_WELCOME_SCREEN_CONFIGURE, AllIcons.General.GearPlain, true));
    toolbar.add(createActionLink("Get Help", IdeActions.GROUP_WELCOME_SCREEN_DOC, null, false));

    panel.add(toolbar, BorderLayout.EAST);


    panel.setBorder(JBUI.Borders.empty(0, 0, 8, 11));
    return panel;
  }

  private JComponent createEventsLink() {
    final Ref<ActionLink> actionLinkRef = new Ref<ActionLink>();
    final JComponent panel = createActionLink("Events", AllIcons.Ide.Notification.NoEvents, actionLinkRef, new AnAction() {
      @RequiredDispatchThread
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        ((WelcomeBalloonLayoutImpl)myFlatWelcomeFrame.myBalloonLayout).showPopup();
      }
    });
    panel.setVisible(false);
    myEventListener = new ParameterizedRunnable<List<NotificationType>>() {
      @Override
      public void run(List<NotificationType> types) {
        NotificationType type1 = null;
        for (NotificationType t : types) {
          if (NotificationType.ERROR == t) {
            type1 = NotificationType.ERROR;
            break;
          }
          if (NotificationType.WARNING == t) {
            type1 = NotificationType.WARNING;
          }
          else if (type1 == null && NotificationType.INFORMATION == t) {
            type1 = NotificationType.INFORMATION;
          }
        }

        actionLinkRef.get().setIcon(IdeNotificationArea.createIconWithNotificationCount(actionLinkRef.get(), type1, types.size()));
        panel.setVisible(true);
      }
    };
    myEventLocation = new Computable<Point>() {
      @Override
      public Point compute() {
        Point location = SwingUtilities.convertPoint(panel, 0, 0, getRootPane().getLayeredPane());
        return new Point(location.x, location.y + 5);
      }
    };
    return panel;
  }

  private JComponent createActionLink(final String text, final String groupId, Icon icon, boolean focusListOnLeft) {
    final Ref<ActionLink> ref = new Ref<ActionLink>(null);
    AnAction action = new AnAction() {
      @RequiredDispatchThread
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        ActionGroup configureGroup = (ActionGroup)ActionManager.getInstance().getAction(groupId);
        final PopupFactoryImpl.ActionGroupPopup popup = (PopupFactoryImpl.ActionGroupPopup)JBPopupFactory.getInstance()
                .createActionGroupPopup(null, new IconsFreeActionGroup(configureGroup), e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                        false, ActionPlaces.WELCOME_SCREEN);
        popup.showUnderneathOfLabel(ref.get());
        UsageTrigger.trigger("welcome.screen." + groupId);
      }
    };
    JComponent panel = createActionLink(text, icon, ref, action);
    installFocusable(panel, action, KeyEvent.VK_UP, KeyEvent.VK_DOWN, focusListOnLeft);
    return panel;
  }

  private JComponent createActionLink(String text, Icon icon, Ref<ActionLink> ref, AnAction action) {
    ActionLink link = new ActionLink(text, icon, action);
    ref.set(link);
    // Don't allow focus, as the containing panel is going to focusable.
    link.setFocusable(false);
    link.setPaintUnderline(false);
    link.setNormalColor(FlatWelcomeFrame.getLinkNormalColor());
    JActionLinkPanel panel = new JActionLinkPanel(link);
    panel.setBorder(JBUI.Borders.empty(4, 6, 4, 6));
    panel.add(createArrow(link), BorderLayout.EAST);
    return panel;
  }


  /**
   * Wraps an {@link com.intellij.ui.components.labels.ActionLink} component and delegates accessibility support to it.
   */
  public static class JActionLinkPanel extends JPanel {
    @NotNull private ActionLink myActionLink;

    public JActionLinkPanel(@NotNull ActionLink actionLink) {
      super(new BorderLayout());
      myActionLink = actionLink;
      add(myActionLink);
      NonOpaquePanel.setTransparent(this);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new JActionLinkPanel.AccessibleJActionLinkPanel(myActionLink.getAccessibleContext());
      }
      return accessibleContext;
    }

    protected class AccessibleJActionLinkPanel extends AccessibleContextDelegate {
      public AccessibleJActionLinkPanel(AccessibleContext context) {
        super(context);
      }

      @Override
      public Accessible getAccessibleParent() {
        if (getParent() instanceof Accessible) {
          return (Accessible)getParent();
        }
        return super.getAccessibleParent();
      }

      @Override
      public AccessibleRole getAccessibleRole() {
        return AccessibleRole.PUSH_BUTTON;
      }
    }
  }

  public static JLabel createArrow(final ActionLink link) {
    JLabel arrow = new JLabel(AllIcons.General.Combo3);
    arrow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    arrow.setVerticalAlignment(SwingConstants.BOTTOM);
    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        final MouseEvent newEvent = MouseEventAdapter.convert(e, link, e.getX(), e.getY());
        link.doClick(newEvent);
        return true;
      }
    }.installOn(arrow);
    return arrow;
  }

  public void installFocusable(final JComponent comp, final AnAction action, final int prevKeyCode, final int nextKeyCode, final boolean focusListOnLeft) {
    comp.setFocusable(true);
    comp.setFocusTraversalKeysEnabled(true);
    comp.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        final JList list = UIUtil.findComponentOfType(myFlatWelcomeFrame.getComponent(), JList.class);
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
          InputEvent event = e;
          if (e.getComponent() instanceof JComponent) {
            ActionLink link = UIUtil.findComponentOfType((JComponent)e.getComponent(), ActionLink.class);
            if (link != null) {
              event = new MouseEvent(link, MouseEvent.MOUSE_CLICKED, e.getWhen(), e.getModifiers(), 0, 0, 1, false, MouseEvent.BUTTON1);
            }
          }
          action.actionPerformed(AnActionEvent.createFromAnAction(action, event, ActionPlaces.WELCOME_SCREEN, DataManager.getInstance().getDataContext()));
        }
        else if (e.getKeyCode() == prevKeyCode) {
          focusPrev(comp);
        }
        else if (e.getKeyCode() == nextKeyCode) {
          focusNext(comp);
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
          if (focusListOnLeft) {
            if (list != null) {
              IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(list);
            }
          }
          else {
            focusPrev(comp);
          }
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
          focusNext(comp);
        }
      }
    });
    comp.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        comp.setOpaque(true);
        comp.setBackground(FlatWelcomeFrame.getActionLinkSelectionColor());
      }

      @Override
      public void focusLost(FocusEvent e) {
        comp.setOpaque(false);
        // comp.setBackground(FlatWelcomeFrame.getMainBackground());
      }
    });

  }

  protected void focusPrev(JComponent comp) {
    FocusTraversalPolicy policy = myFlatWelcomeFrame.getFocusTraversalPolicy();
    if (policy != null) {
      Component prev = policy.getComponentBefore(myFlatWelcomeFrame, comp);
      if (prev != null) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
          IdeFocusManager.getGlobalInstance().requestFocus(prev, true);
        });
      }
    }
  }

  protected void focusNext(JComponent comp) {
    FocusTraversalPolicy policy = myFlatWelcomeFrame.getFocusTraversalPolicy();
    if (policy != null) {
      Component next = policy.getComponentAfter(myFlatWelcomeFrame, comp);
      if (next != null) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
          IdeFocusManager.getGlobalInstance().requestFocus(next, true);
        });
      }
    }
  }

  private class IconsFreeActionGroup extends ActionGroup {
    private final ActionGroup myGroup;

    public IconsFreeActionGroup(ActionGroup group) {
      super(group.getTemplatePresentation().getText(), group.getTemplatePresentation().getDescription(), null);
      myGroup = group;
    }

    @Override
    public boolean isPopup() {
      return myGroup.isPopup();
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      AnAction[] children = myGroup.getChildren(e);
      AnAction[] patched = new AnAction[children.length];
      for (int i = 0; i < children.length; i++) {
        patched[i] = patch(children[i]);
      }
      return patched;
    }

    private AnAction patch(final AnAction child) {
      if (child instanceof ActionGroup) {
        return new IconsFreeActionGroup((ActionGroup)child);
      }

      Presentation presentation = child.getTemplatePresentation();
      return new AnAction(presentation.getText(), presentation.getDescription(), null) {
        @RequiredDispatchThread
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          child.actionPerformed(e);
          UsageTrigger.trigger("welcome.screen." + e.getActionManager().getId(child));
        }

        @RequiredDispatchThread
        @Override
        public void update(@NotNull AnActionEvent e) {
          child.update(e);
          e.getPresentation().setIcon(null);
        }

        @Override
        public boolean isDumbAware() {
          return child.isDumbAware();
        }
      };
    }
  }
}
