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
package consulo.desktop.awt.welcomeScreen;

import consulo.application.ApplicationProperties;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.startup.splash.AnimatedLogoLabel;
import consulo.disposer.Disposable;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ide.impl.welcomeScreen.BaseWelcomeScreenPanel;
import consulo.ide.impl.welcomeScreen.WelcomeScreenConstants;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.NotificationIconBuilder;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextDelegate;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Konstantin Bulenkov
 */
public abstract class FlatWelcomePanel extends BaseWelcomeScreenPanel {
  private FlatWelcomeFrame myFlatWelcomeFrame;
  public Consumer<List<NotificationType>> myEventListener;
  public Supplier<Point> myEventLocation;

  @RequiredUIAccess
  public FlatWelcomePanel(FlatWelcomeFrame flatWelcomeFrame, TitlelessDecorator titlelessDecorator) {
    super(flatWelcomeFrame, titlelessDecorator);
    myFlatWelcomeFrame = flatWelcomeFrame;
  }

  @RequiredUIAccess
  public abstract JComponent createActionPanel();

  @Nonnull
  @Override
  protected JComponent createLeftComponent(@Nonnull Disposable parentDisposable) {
    return new NewRecentProjectPanel(parentDisposable, true).getRootPanel();
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  protected JComponent createRightComponent() {
    JPanel panel = new JPanel(new BorderLayout());
    JPanel logoPanel = new JPanel(new BorderLayout());
    logoPanel.setBorder(JBUI.Borders.empty(53, 22, 45, 0));
    Color foreground = MorphColor.ofWithoutCache(() -> ApplicationProperties.isInSandbox()
      ? StyleManager.get().getCurrentStyle().isDark() ? JBColor.LIGHT_GRAY : JBColor.WHITE
      : JBColor.GRAY
    );
    AnimatedLogoLabel animatedLogoLabel = new AnimatedLogoLabel(8, foreground, false, true);
    logoPanel.add(animatedLogoLabel, BorderLayout.CENTER);

    JPanel topPanel = new JPanel(new VerticalFlowLayout(true, false));
    topPanel.add(logoPanel);
    topPanel.add(createActionPanel());

    panel.add(topPanel, BorderLayout.NORTH);
    panel.add(createSettingsAndDocs(), BorderLayout.SOUTH);
    return panel;
  }

  private JComponent createSettingsAndDocs() {
    JPanel panel = new NonOpaquePanel(new BorderLayout());
    NonOpaquePanel toolbar = new NonOpaquePanel();

    toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
    toolbar.add(createEventsLink());
    toolbar.add(createActionLink("Configure", IdeActions.GROUP_WELCOME_SCREEN_CONFIGURE, PlatformIconGroup.generalSettings(), true));
    toolbar.add(createActionLink("Get Help", IdeActions.GROUP_WELCOME_SCREEN_DOC, PlatformIconGroup.actionsHelp(), false));

    panel.add(toolbar, BorderLayout.EAST);

    panel.setBorder(JBUI.Borders.empty(0, 0, 8, 11));
    return panel;
  }

  private JComponent createEventsLink() {
    final SimpleReference<ActionLink> actionLinkRef = new SimpleReference<>();
    final JComponent panel = createActionLink("Events", PlatformIconGroup.toolwindowsNotifications(), actionLinkRef, new AnAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        ((WelcomeDesktopBalloonLayoutImpl)myFlatWelcomeFrame.getBalloonLayout()).showPopup();
      }
    });
    panel.setVisible(false);
    myEventListener = types -> {
      actionLinkRef.get().setIcon(TargetAWT.to(NotificationIconBuilder.getIcon(types)));
      panel.setVisible(true);
    };
    myEventLocation = () -> {
      Point location = SwingUtilities.convertPoint(panel, 0, 0, getRootPane().getLayeredPane());
      return new Point(location.x, location.y + 5);
    };
    return panel;
  }

  private JComponent createActionLink(final String text, final String groupId, Image icon, boolean focusListOnLeft) {
    final SimpleReference<ActionLink> ref = new SimpleReference<>(null);
    AnAction action = new AnAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        MouseEvent inputEvent = (MouseEvent)e.getInputEvent();
        ActionGroup configureGroup = (ActionGroup)ActionManager.getInstance().getAction(groupId);
        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("WelcomeActions", configureGroup);
        menu.getComponent().show(ref.get(), inputEvent.getX(), inputEvent.getY() + ref.get().getHeight());
        UsageTrigger.trigger("welcome.screen." + groupId);
      }
    };
    JComponent panel = createActionLink(text, icon, ref, action);
    installFocusable(panel, action, KeyEvent.VK_UP, KeyEvent.VK_DOWN, focusListOnLeft);
    return panel;
  }

  private JComponent createActionLink(String text, Image icon, SimpleReference<ActionLink> ref, AnAction action) {
    ActionLink link = new ActionLink(text, icon, action);
    ref.set(link);
    // Don't allow focus, as the containing panel is going to focusable.
    link.setFocusable(false);
    link.setPaintUnderline(false);
    link.setNormalColor(WelcomeScreenConstants.getLinkNormalColor());
    JActionLinkPanel panel = new JActionLinkPanel(link);
    panel.setBorder(JBUI.Borders.empty(4, 6, 4, 6));
    return panel;
  }


  /**
   * Wraps an {@link ActionLink} component and delegates accessibility support to it.
   */
  public static class JActionLinkPanel extends JPanel {
    @Nonnull
    private ActionLink myActionLink;

    public JActionLinkPanel(@Nonnull ActionLink actionLink) {
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
      public Container getDelegateParent() {
        return getParent();
      }

      @Override
      public AccessibleRole getAccessibleRole() {
        return AccessibleRole.PUSH_BUTTON;
      }
    }
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
        comp.setBackground(WelcomeScreenConstants.getActionLinkSelectionColor());
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
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(prev, true));
      }
    }
  }

  protected void focusNext(JComponent comp) {
    FocusTraversalPolicy policy = myFlatWelcomeFrame.getFocusTraversalPolicy();
    if (policy != null) {
      Component next = policy.getComponentAfter(myFlatWelcomeFrame, comp);
      if (next != null) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(next, true));
      }
    }
  }
}
