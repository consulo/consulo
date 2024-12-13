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
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.application.ui.FrameStateManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.desktop.awt.uiOld.BalloonLayoutConfiguration;
import consulo.desktop.awt.uiOld.BalloonLayoutData;
import consulo.desktop.awt.uiOld.DesktopBalloonLayoutImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.notification.impl.NotificationSettings;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.impl.idea.ui.BalloonImpl;
import consulo.ide.impl.idea.ui.components.GradientViewport;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.startup.StartupManager;
import consulo.project.ui.internal.BalloonLayoutEx;
import consulo.project.ui.internal.NotificationActionProvider;
import consulo.project.ui.internal.NotificationFullContent;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.NotificationsManager;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.wm.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.DialogWrapperDialog;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StyleManager;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.UIResource;
import javax.swing.text.*;
import javax.swing.text.html.ParagraphView;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author spleaner
 */
@Singleton
@ServiceImpl
public class NotificationsManagerImpl extends NotificationsManager {
  private static final Logger LOG = Logger.getInstance(NotificationsManagerImpl.class);

  public static final Color DEFAULT_TEXT_COLOR = MorphColor.of(UIUtil::getLabelForeground);
  public static final Color FILL_COLOR = MorphColor.of(UIUtil::getPanelBackground);
  public static final Color BORDER_COLOR = MorphColor.of(UIUtil::getBorderColor);

  private final Application myApplication;

  @Inject
  public NotificationsManagerImpl(Application application) {
    myApplication = application;
  }

  @Override
  public void expire(@Nonnull final Notification notification) {
    myApplication.getLastUIAccess().give(() -> EventLog.expireNotification(notification));
  }

  @Override
  @Nonnull
  public <T extends Notification> T[] getNotificationsOfType(@Nonnull Class<T> klass, @Nullable final Project project) {
    final List<T> result = new ArrayList<>();
    if (project == null || !project.isDefault() && !project.isDisposed()) {
      for (Notification notification : EventLog.getLogModel(project).getNotifications()) {
        if (klass.isInstance(notification)) {
          //noinspection unchecked
          result.add((T)notification);
        }
      }
    }
    return ArrayUtil.toObjectArray(result, klass);
  }

  public void doNotify(@Nonnull final Notification notification, @Nullable final Project project) {
    final NotificationsConfigurationImpl configuration = NotificationsConfigurationImpl.getInstanceImpl();
    if (!configuration.isRegistered(notification.getGroupId())) {
      LOG.error("NotificationGroup: " + notification.getGroupId() + " is not registered. Please use NotificationGroupContributor");
    }

    final NotificationSettings settings = NotificationsConfigurationImpl.getSettings(notification.getGroupId());
    boolean shouldLog = settings.isShouldLog();
    boolean displayable = settings.getDisplayType() != NotificationDisplayType.NONE;

    boolean willBeShown = displayable && NotificationsConfigurationImpl.getInstanceImpl().SHOW_BALLOONS;
    if (!shouldLog && !willBeShown) {
      notification.expire();
    }

    if (NotificationsConfigurationImpl.getInstanceImpl().SHOW_BALLOONS) {
      final DumbAwareRunnable runnable = () -> showNotification(notification, project);
      if (project == null) {
        UIUtil.invokeLaterIfNeeded(runnable);
      }
      else if (!project.isDisposed()) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(runnable);
      }
    }
  }

  @RequiredUIAccess
  private void showNotification(@Nonnull final Notification notification, @Nullable final Project project) {
    Window window = findWindowForBalloon(project);
    if (window == null) {
      UIAccess uiAccess = UIAccess.current();
      AppExecutorUtil.getAppScheduledExecutorService()
        .schedule(() -> uiAccess.give(() -> showNotification(notification, project)), 1L, TimeUnit.SECONDS);
      return;
    }


    String groupId = notification.getGroupId();
    final NotificationSettings settings = NotificationsConfigurationImpl.getSettings(groupId);

    NotificationDisplayType type = settings.getDisplayType();
    String toolWindowId = NotificationsConfigurationImpl.getInstanceImpl().getToolWindowId(groupId);
    if (type == NotificationDisplayType.TOOL_WINDOW && (toolWindowId == null || project == null || !ToolWindowManager.getInstance(project)
                                                                                                                     .canShowNotification(
                                                                                                                       toolWindowId))) {
      type = NotificationDisplayType.BALLOON;
    }

    switch (type) {
      case NONE:
        return;
      //case EXTERNAL:
      //  notifyByExternal(notification);
      //  break;
      case STICKY_BALLOON:
      case BALLOON:
      default:
        Balloon balloon = notifyByBalloon(notification, type, project);
        if (project == null || project.isDefault()) {
          return;
        }
        if (!settings.isShouldLog() || type == NotificationDisplayType.STICKY_BALLOON) {
          if (balloon == null) {
            notification.expire();
          }
          else {
            balloon.addListener(new JBPopupAdapter() {
              @Override
              public void onClosed(LightweightWindowEvent event) {
                if (!event.isOk()) {
                  notification.expire();
                }
              }
            });
          }
        }
        break;
      case TOOL_WINDOW:
        NotificationType messageType = notification.getType();
        final NotificationListener notificationListener = notification.getListener();
        HyperlinkListener listener = notificationListener == null ? null : e -> notificationListener.hyperlinkUpdate(notification, e);
        assert toolWindowId != null;
        String msg = notification.getTitle();
        if (StringUtil.isNotEmpty(notification.getContent())) {
          if (StringUtil.isNotEmpty(msg)) {
            msg += "<br>";
          }
          msg += notification.getContent();
        }

        IdeFrame ideFrame = findIdeFrameForBalloon(project);
        if (ideFrame != null) {
          BalloonLayout layout = ideFrame.getBalloonLayout();
          if (layout != null) {
            ((BalloonLayoutEx)layout).remove(notification);
          }
        }

        //noinspection SSBasedInspection
        ToolWindowManager.getInstance(project).notifyByBalloon(toolWindowId, messageType.toUI(), msg, notification.getIcon(), listener);
    }
  }

  @Nullable
  @RequiredUIAccess
  private Balloon notifyByBalloon(
    @Nonnull final Notification notification,
    @Nonnull final NotificationDisplayType displayType,
    @Nullable final Project project
  ) {
    IdeFrame ideFrame = findIdeFrameForBalloon(project);
    if (ideFrame != null) {
      BalloonLayout layout = ideFrame.getBalloonLayout();
      if (layout == null) return null;

      final ProjectManager projectManager = ProjectManager.getInstance();
      final boolean noProjects = projectManager.getOpenProjects().length == 0;
      final boolean sticky = NotificationDisplayType.STICKY_BALLOON == displayType || noProjects;
      Ref<Object> layoutDataRef = Ref.create();
      if (project == null || project.isDefault()) {
        BalloonLayoutData layoutData = new BalloonLayoutData();
        layoutData.groupId = "";
        layoutData.welcomeScreen = layout.isForWelcomeFrame();
        layoutData.type = notification.getType();
        layoutDataRef.set(layoutData);
      }
      else {
        BalloonLayoutData.MergeInfo mergeData = ((DesktopBalloonLayoutImpl)layout).preMerge(notification);
        if (mergeData != null) {
          BalloonLayoutData layoutData = new BalloonLayoutData();
          layoutData.mergeData = mergeData;
          layoutDataRef.set(layoutData);
        }
      }
      final Balloon balloon =
        createBalloon(ideFrame, notification, false, false, layoutDataRef, project != null ? project : myApplication);
      if (notification.isExpired()) {
        return null;
      }

      layout.add(balloon, layoutDataRef.get());
      if (layoutDataRef.get() != null) {
        ((BalloonLayoutData)layoutDataRef.get()).project = project;
      }
      ((BalloonImpl)balloon).startFadeoutTimer(0);
      if (NotificationDisplayType.BALLOON == displayType) {
        FrameStateManager.getInstance().getApplicationActive().doWhenDone(() -> {
          if (balloon.isDisposed()) {
            return;
          }

          if (!sticky) {
            ((BalloonImpl)balloon).startSmartFadeoutTimer(10000);
          }
        });
      }
      return balloon;
    }
    return null;
  }

  @Override
  @Nullable
  @RequiredUIAccess
  public Window findWindowForBalloon(@Nullable Project project) {
    Window frame = TargetAWT.to(WindowManager.getInstance().getWindow(project));
    if (frame == null && project == null) {
      IdeFrame currentFrame = WelcomeFrameManager.getInstance().getCurrentFrame();
      frame = currentFrame == null ? null : TargetAWT.to(currentFrame.getWindow());
    }
    if (frame == null && project == null) {
      frame = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      while (frame instanceof DialogWrapperDialog dialog && dialog.getDialogWrapper().isModalProgress()) {
        frame = frame.getOwner();
      }
    }
    return frame;
  }

  @Nullable
  @RequiredUIAccess
  public IdeFrame findIdeFrameForBalloon(@Nullable Project project) {
    Window windowForBalloon = findWindowForBalloon(project);
    consulo.ui.Window uiWindow = TargetAWT.from(windowForBalloon);
    return uiWindow == null ? null : uiWindow.getUserData(IdeFrame.KEY);
  }

  @Override
  @Nonnull
  public Balloon createBalloon(
    @Nullable final JComponent windowComponent,
    @Nonnull final Notification notification,
    final boolean showCallout,
    final boolean hideOnClickOutside,
    @Nonnull Ref<Object> layoutDataRef,
    @Nonnull Disposable parentDisposable
  ) {
    final BalloonLayoutData layoutData = layoutDataRef.isNull() ? new BalloonLayoutData() : (BalloonLayoutData)layoutDataRef.get();
    if (layoutData.groupId == null) {
      layoutData.groupId = notification.getGroupId();
      layoutData.id = notification.id;
    }
    else {
      layoutData.groupId = null;
      layoutData.mergeData = null;
    }
    layoutDataRef.set(layoutData);

    if (layoutData.textColor == null) {
      layoutData.textColor = JBColor.namedColor("Notification.foreground", DEFAULT_TEXT_COLOR);
    }

    if (layoutData.fillColor == null) {
      layoutData.fillColor = FILL_COLOR;
    }
    if (layoutData.borderColor == null) {
      layoutData.borderColor = BORDER_COLOR;
    }

    boolean actions = !notification.getActions().isEmpty();
    boolean showFullContent = layoutData.showFullContent || notification instanceof NotificationFullContent;

    final JEditorPane text = new JEditorPane() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (layoutData.showMinSize) {
          Point location = getCollapsedTextEndLocation(this, layoutData);
          if (location != null) {
            g.setColor(getForeground());
            g.drawString("...", location.x, location.y + g.getFontMetrics().getAscent());
          }
        }
      }
    };
    JBHtmlEditorKit kit = new JBHtmlEditorKit() {
      final HTMLFactory factory = new HTMLFactory() {
        @Override
        public View create(Element e) {
          View view = super.create(e);
          if (view instanceof ParagraphView) {
            // wrap too long words, for example: ATEST_TABLE_SIGNLE_ROW_UPDATE_AUTOCOMMIT_A_FIK
            return new ParagraphView(e) {
              @Override
              protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
                if (r == null) {
                  r = new SizeRequirements();
                }
                r.minimum = (int)layoutPool.getMinimumSpan(axis);
                r.preferred = Math.max(r.minimum, (int)layoutPool.getPreferredSpan(axis));
                r.maximum = Integer.MAX_VALUE;
                r.alignment = 0.5f;
                return r;
              }
            };
          }
          return view;
        }
      };

      @Override
      public ViewFactory getViewFactory() {
        return factory;
      }
    };
    kit.getStyleSheet().addRule("a {color: " + ColorUtil.toHtmlColor(JBCurrentTheme.Link.linkColor()) + "}");
    text.setEditorKit(kit);
    text.setForeground(layoutData.textColor);

    final HyperlinkListener listener = NotificationsUtil.wrapListener(notification);
    if (listener != null) {
      text.addHyperlinkListener(listener);
    }

    String fontStyle = NotificationsUtil.getFontStyle();
    int prefSize = new JLabel(NotificationsUtil.buildHtml(notification, null, true, null, fontStyle)).getPreferredSize().width;
    String style = prefSize > BalloonLayoutConfiguration.MaxWidth() ? BalloonLayoutConfiguration.MaxWidthStyle() : null;

    if (layoutData.showFullContent) {
      style = prefSize > BalloonLayoutConfiguration.MaxFullContentWidth() ? BalloonLayoutConfiguration.MaxFullContentWidthStyle() : null;
    }

    String textR = NotificationsUtil.buildHtml(notification, style, true, layoutData.textColor, fontStyle);
    String textD = NotificationsUtil.buildHtml(notification, style, true, layoutData.textColor, fontStyle);
    LafHandler lafHandler = new LafHandler(text, textR, textD);
    layoutData.lafHandler = lafHandler;

    text.setEditable(false);
    text.setOpaque(false);

    text.setBorder(null);

    final JPanel content = new NonOpaquePanel(new BorderLayout());

    if (text.getCaret() != null) {
      text.setCaretPosition(0);
    }

    final JScrollPane pane = createBalloonScrollPane(text, false);

    pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        JScrollBar scrollBar = pane.getVerticalScrollBar();
        if (layoutData.showMinSize && scrollBar.getValue() > 0) {
          scrollBar.removeAdjustmentListener(this);
          scrollBar.setValue(0);
          scrollBar.addAdjustmentListener(this);
        }
      }
    });

    LinkLabel<Void> expandAction = null;

    int lines = 3;
    if (notification.hasTitle()) {
      lines--;
    }
    if (actions) {
      lines--;
    }

    layoutData.fullHeight = text.getPreferredSize().height;
    layoutData.twoLineHeight = calculateContentHeight(lines);
    layoutData.maxScrollHeight = Math.min(layoutData.fullHeight, calculateContentHeight(10));
    layoutData.configuration = BalloonLayoutConfiguration.create(notification, layoutData, actions);

    if (layoutData.welcomeScreen) {
      layoutData.maxScrollHeight = layoutData.fullHeight;
    }
    else if (!showFullContent && layoutData.maxScrollHeight != layoutData.fullHeight) {
      pane.setViewport(new GradientViewport(text, JBUI.insets(10, 0), true) {
        @Nullable
        @Override
        protected Color getViewColor() {
          return layoutData.fillColor;
        }

        @Override
        protected void paintGradient(Graphics g) {
          if (!layoutData.showMinSize) {
            super.paintGradient(g);
          }
        }
      });
    }

    configureBalloonScrollPane(pane, layoutData.fillColor);

    if (showFullContent) {
      pane.setPreferredSize(text.getPreferredSize());
    }
    else if (layoutData.twoLineHeight < layoutData.fullHeight) {
      text.setPreferredSize(null);
      Dimension size = text.getPreferredSize();
      size.height = layoutData.twoLineHeight;
      text.setPreferredSize(size);
      text.setSize(size);
      layoutData.showMinSize = true;

      pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
      pane.setPreferredSize(size);

      text.setCaret(new TextCaret(layoutData));

      expandAction = new LinkLabel<>(null, AllIcons.Ide.Notification.Expand, (LinkListener<Void>)(link, ignored) -> {
        layoutData.showMinSize = !layoutData.showMinSize;

        text.setPreferredSize(null);
        Dimension size1 = text.getPreferredSize();

        if (layoutData.showMinSize) {
          size1.height = layoutData.twoLineHeight;
          pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
          link.setIcon(TargetAWT.to(AllIcons.Ide.Notification.Expand));
          link.setHoveringIcon(TargetAWT.to(AllIcons.Ide.Notification.ExpandHover));
        }
        else {
          text.select(0, 0);
          size1.height = layoutData.fullHeight;
          pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
          link.setIcon(TargetAWT.to(AllIcons.Ide.Notification.Collapse));
          link.setHoveringIcon(TargetAWT.to(AllIcons.Ide.Notification.CollapseHover));
        }

        text.setPreferredSize(size1);
        text.setSize(size1);

        if (!layoutData.showMinSize) {
          size1 = new Dimension(size1.width, layoutData.maxScrollHeight);
        }
        pane.setPreferredSize(size1);

        content.doLayout();
        layoutData.doLayout.run();
      });
      expandAction.setHoveringIcon(TargetAWT.to(AllIcons.Ide.Notification.ExpandHover));
    }

    final CenteredLayoutWithActions layout = new CenteredLayoutWithActions(text, layoutData);
    JPanel centerPanel = new NonOpaquePanel(layout) {
      @Override
      protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        Component title = layout.getTitle();

        if (title != null && layoutData.showActions != null && layoutData.showActions.compute()) {
          int width = layoutData.configuration.allActionsOffset;
          int x = getWidth() - width - JBUI.scale(5);
          int y = layoutData.configuration.topSpaceHeight;

          int height = title instanceof JEditorPane jEditorPane ? getFirstLineHeight(jEditorPane) : title.getHeight();

          g.setColor(layoutData.fillColor);
          g.fillRect(x, y, width, height);

          width = layoutData.configuration.beforeGearSpace;
          x -= width;
          ((Graphics2D)g).setPaint(
            new GradientPaint(x, y, ColorUtil.withAlpha(layoutData.fillColor, 0.2), x + width, y, layoutData.fillColor)
          );
          g.fillRect(x, y, width, height);
        }
      }
    };
    content.add(centerPanel, BorderLayout.CENTER);

    if (notification.hasTitle()) {
      String titleStyle = StringUtil.defaultIfEmpty(fontStyle, "") + "white-space:nowrap;";
      String titleR = NotificationsUtil.buildHtml(notification, titleStyle, false, layoutData.textColor, null);
      String titleD = NotificationsUtil.buildHtml(notification, titleStyle, false, layoutData.textColor, null);
      JLabel title = new JLabel();
      lafHandler.setTitle(title, titleR, titleD);
      title.setOpaque(false);
      title.setForeground(layoutData.textColor);
      centerPanel.add(title, BorderLayout.NORTH);
    }

    if (expandAction != null) {
      centerPanel.add(expandAction, BorderLayout.EAST);
    }

    if (notification.hasContent()) {
      centerPanel.add(layoutData.welcomeScreen ? text : pane, BorderLayout.CENTER);
    }

    if (!layoutData.welcomeScreen) {
      final Icon icon = TargetAWT.to(NotificationsUtil.getIcon(notification));
      JComponent iconComponent = new JComponent() {
        @Override
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          icon.paintIcon(this, g, layoutData.configuration.iconOffset.width, layoutData.configuration.iconOffset.height);
        }
      };
      iconComponent.setOpaque(false);
      iconComponent.setPreferredSize(new Dimension(layoutData.configuration.iconPanelWidth,
                                                   2 * layoutData.configuration.iconOffset.height + icon.getIconHeight()));

      content.add(iconComponent, BorderLayout.WEST);
    }

    JPanel buttons = createButtons(notification, content, listener);
    if (buttons != null) {
      layoutData.groupId = null;
      layoutData.mergeData = null;
      buttons.setBorder(new EmptyBorder(0, 0, JBUI.scale(5), JBUI.scale(7)));
    }

    HoverAdapter hoverAdapter = new HoverAdapter();
    hoverAdapter.addSource(content);
    hoverAdapter.addSource(centerPanel);
    hoverAdapter.addSource(text);
    hoverAdapter.addSource(pane);

    if (buttons == null && actions) {
      createActionPanel(notification, centerPanel, layoutData.configuration.actionGap, hoverAdapter);
    }

    if (expandAction != null) {
      hoverAdapter.addComponent(expandAction, component -> {
        Rectangle bounds;
        Point location = SwingUtilities.convertPoint(content.getParent(), content.getLocation(), component.getParent());
        if (layoutData.showMinSize) {
          Component centerComponent = layoutData.welcomeScreen ? text : pane;
          Point centerLocation =
            SwingUtilities.convertPoint(centerComponent.getParent(), centerComponent.getLocation(), component.getParent());
          bounds = new Rectangle(location.x, centerLocation.y, content.getWidth(), centerComponent.getHeight());
        }
        else {
          bounds = new Rectangle(location.x, component.getY(), content.getWidth(), component.getHeight());
          JBInsets.addTo(bounds, JBUI.insets(5, 0, 7, 0));
        }
        return bounds;
      });
    }

    hoverAdapter.initListeners();

    if (layoutData.mergeData != null) {
      createMergeAction(layoutData, content);
    }

    text.setSize(text.getPreferredSize());

    Dimension paneSize = new Dimension(text.getPreferredSize());
    int maxWidth = JBUI.scale(600);
    if (windowComponent != null) {
      maxWidth = Math.min(maxWidth, windowComponent.getWidth() - 20);
    }
    if (paneSize.width > maxWidth) {
      pane.setPreferredSize(new Dimension(maxWidth, paneSize.height + UIUtil.getScrollBarWidth()));
    }

    final BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(content);
    builder.setFillColor(layoutData.fillColor)
           .setCloseButtonEnabled(buttons == null)
           .setShowCallout(showCallout)
           .setShadow(false)
           .setAnimationCycle(200)
           .setHideOnClickOutside(hideOnClickOutside)
           .setHideOnAction(hideOnClickOutside)
           .setHideOnKeyOutside(hideOnClickOutside)
           .setHideOnFrameResize(false)
           .setBorderColor(layoutData.borderColor)
           .setBorderInsets(JBUI.emptyInsets());

    if (layoutData.fadeoutTime != 0) {
      builder.setFadeoutTime(layoutData.fadeoutTime);
    }

    final BalloonImpl balloon = (BalloonImpl)builder.createBalloon();
    balloon.setAnimationEnabled(false);
    notification.setBalloon(balloon);

    balloon.setShadowBorderProvider(new NotificationBalloonShadowBorderProvider(layoutData.fillColor, layoutData.borderColor));

    if (!layoutData.welcomeScreen && buttons == null) {
      balloon.setActionProvider(new NotificationBalloonActionProvider(balloon, layout.getTitle(), layoutData, notification.getGroupId()));
    }

    Disposer.register(parentDisposable, balloon);
    return balloon;
  }

  @Nullable
  private static JPanel createButtons(
    @Nonnull Notification notification,
    @Nonnull final JPanel content,
    @Nullable HyperlinkListener listener
  ) {
    if (notification instanceof NotificationActionProvider provider) {
      JPanel buttons = new JPanel(new HorizontalLayout(5));
      buttons.setOpaque(false);
      content.add(BorderLayout.SOUTH, buttons);

      final Ref<JButton> defaultButton = new Ref<>();

      for (NotificationActionProvider.Action action : provider.getActions(listener)) {
        JButton button = new JButton(action);

        button.setOpaque(false);
        if (action.isDefaultAction()) {
          defaultButton.setIfNull(button);
        }

        buttons.add(HorizontalLayout.RIGHT, button);
      }

      if (!defaultButton.isNull()) {
        UIUtil.addParentChangeListener(content, new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent event) {
            if (event.getOldValue() == null && event.getNewValue() != null) {
              UIUtil.removeParentChangeListener(content, this);
              JRootPane rootPane = UIUtil.getRootPane(content);
              if (rootPane != null) {
                rootPane.setDefaultButton(defaultButton.get());
              }
            }
          }
        });
      }

      return buttons;
    }
    return null;
  }

  @Nonnull
  public static JScrollPane createBalloonScrollPane(@Nonnull Component content, boolean configure) {
    JScrollPane pane = ScrollPaneFactory.createScrollPane(content, true);
    if (configure) {
      configureBalloonScrollPane(pane, FILL_COLOR);
    }
    return pane;
  }

  public static void configureBalloonScrollPane(@Nonnull JScrollPane pane, @Nonnull Color fillColor) {
    pane.setOpaque(false);
    pane.getViewport().setOpaque(false);
    pane.setBackground(fillColor);
    pane.getViewport().setBackground(fillColor);
    pane.getVerticalScrollBar().setBackground(fillColor);
  }

  private static void createActionPanel(@Nonnull final Notification notification,
                                        @Nonnull JPanel centerPanel,
                                        int gap,
                                        @Nonnull HoverAdapter hoverAdapter) {
    JPanel actionPanel = new NonOpaquePanel(new HorizontalLayout(gap, SwingConstants.CENTER));
    centerPanel.add(BorderLayout.SOUTH, actionPanel);

    List<AnAction> actions = notification.getActions();

    if (actions.size() > 2) {
      DropDownAction action = new DropDownAction(notification.getDropDownText(), (link, ignored) -> {
        Container parent = link.getParent();
        int size = parent.getComponentCount();
        DefaultActionGroup group = new DefaultActionGroup();
        for (int i = 1; i < size; i++) {
          Component component = parent.getComponent(i);
          if (!component.isVisible()) {
            group.add(((LinkLabel<AnAction>)component).getLinkData());
          }
        }
        showPopup(link, group);
      });
      Notification.setDataProvider(notification, action);
      action.setVisible(false);
      actionPanel.add(action);
    }

    for (AnAction action : actions) {
      Presentation presentation = action.getTemplatePresentation();
      actionPanel.add(HorizontalLayout.LEFT, new LinkLabel<>(presentation.getText(),
                                                             presentation.getIcon(),
                                                             (aSource, action1) -> Notification.fire(notification, action1),
                                                             action));
    }

    Insets hover = JBUI.insets(8, 5, 8, 7);
    int count = actionPanel.getComponentCount();

    for (int i = 0; i < count; i++) {
      hoverAdapter.addComponent(actionPanel.getComponent(i), hover);
    }

    hoverAdapter.addSource(actionPanel);
  }

  private static class HoverAdapter extends MouseAdapter implements MouseMotionListener {
    private final List<Pair<Component, ?>> myComponents = new ArrayList<>();
    private List<Component> mySources = new ArrayList<>();

    private Component myLastComponent;

    public void addComponent(@Nonnull Component component, @Nonnull Function<Component, Rectangle> hover) {
      myComponents.add(Pair.create(component, hover));
    }

    public void addComponent(@Nonnull Component component, @Nonnull Insets hover) {
      myComponents.add(Pair.create(component, hover));
    }

    public void addSource(@Nonnull Component component) {
      mySources.add(component);
    }

    public void initListeners() {
      if (!myComponents.isEmpty()) {
        for (Component source : mySources) {
          source.addMouseMotionListener(this);
          source.addMouseListener(this);
        }
        mySources = null;
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      handleEvent(e, true, false);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      handleEvent(e, false, false);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      handleEvent(e, false, true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      if (myLastComponent != null) {
        mouseExited(e, myLastComponent);
        myLastComponent = null;
      }
    }

    private void handleEvent(MouseEvent e, boolean pressed, boolean moved) {
      for (Pair<Component, ?> p : myComponents) {
        Component component = p.first;
        Rectangle bounds;
        if (p.second instanceof Insets insets) {
          bounds = component.getBounds();
          JBInsets.addTo(bounds, insets);
        }
        else {
          bounds = ((Function<Component, Rectangle>)p.second).apply(component);
        }
        if (bounds.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), component.getParent()))) {
          if (myLastComponent != null && myLastComponent != component) {
            mouseExited(e, myLastComponent);
          }
          myLastComponent = component;

          MouseEvent event = createEvent(e, component);
          if (moved) {
            for (MouseMotionListener listener : component.getMouseMotionListeners()) {
              listener.mouseMoved(event);
            }
          }
          else {
            MouseListener[] listeners = component.getMouseListeners();
            if (pressed) {
              for (MouseListener listener : listeners) {
                listener.mousePressed(event);
              }
            }
            else {
              for (MouseListener listener : listeners) {
                listener.mouseReleased(event);
              }
            }
          }

          e.getComponent().setCursor(component.getCursor());
          return;
        }
        else if (component == myLastComponent) {
          myLastComponent = null;
          mouseExited(e, component);
        }
      }
    }

    private static void mouseExited(MouseEvent e, Component component) {
      e.getComponent().setCursor(null);

      MouseEvent event = createEvent(e, component);
      MouseListener[] listeners = component.getMouseListeners();
      for (MouseListener listener : listeners) {
        listener.mouseExited(event);
      }
    }

    @Nonnull
    private static MouseEvent createEvent(MouseEvent e, Component c) {
      return new MouseEvent(c, e.getID(), e.getWhen(), e.getModifiers(), 5, 5, e.getClickCount(), e.isPopupTrigger(), e.getButton());
    }
  }

  private static void createMergeAction(@Nonnull final BalloonLayoutData layoutData, @Nonnull JPanel panel) {
    StringBuilder title = new StringBuilder().append(layoutData.mergeData.count).append(" more");

    LinkLabel<BalloonLayoutData> action = new LinkLabel<>(title.toString(), null, (aSource, layoutData1) -> EventLog.showNotification(
      layoutData1.project, layoutData1.groupId, layoutData1.getMergeIds()), layoutData) {
      @Override
      protected boolean isInClickableArea(Point pt) {
        return true;
      }

      @Override
      protected Color getTextColor() {
        return new JBColor(0x666666, 0x8C8C8C);
      }
    };

    action.setFont(FontUtil.minusOne(action.getFont()));
    action.setHorizontalAlignment(SwingConstants.CENTER);
    action.setPaintUnderline(false);

    AbstractLayoutManager layout = new AbstractLayoutManager() {
      @Override
      public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(parent.getWidth(), JBUI.scale(20) + 2);
      }

      @Override
      public void layoutContainer(Container parent) {
        parent.getComponent(0).setBounds(2, 1, parent.getWidth() - 4, JBUI.scale(20));
      }
    };
    JPanel mergePanel = new NonOpaquePanel(layout) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new JBColor(0xE3E3E3, 0x3A3C3D));
        ((Graphics2D)g).fill(new Rectangle2D.Double(1.5, 1, getWidth() - 2.5, getHeight() - 2));
        g.setColor(new JBColor(0xDBDBDB, 0x353738));
        if (Platform.current().os().isMac()) {
          ((Graphics2D)g).draw(new Rectangle2D.Double(2, 0, getWidth() - 3.5, 0.5));
        }
        else if (Platform.current().os().isWindows()) {
          ((Graphics2D)g).draw(new Rectangle2D.Double(1.5, 0, getWidth() - 3, 0.5));
        }
        else {
          ((Graphics2D)g).draw(new Rectangle2D.Double(1.5, 0, getWidth() - 2.5, 0.5));
        }
      }
    };
    mergePanel.add(action);
    panel.add(BorderLayout.SOUTH, mergePanel);
  }

  public static int calculateContentHeight(int lines) {
    JEditorPane text = new JEditorPane();
    text.setEditorKit(JBHtmlEditorKit.create());
    text.setText(NotificationsUtil.buildHtml(null,
                                             null,
                                             "Content" + StringUtil.repeat("<br>\nContent", lines - 1),
                                             null,
                                             null,
                                             null,
                                             NotificationsUtil.getFontStyle()));
    text.setEditable(false);
    text.setOpaque(false);
    text.setBorder(null);

    return text.getPreferredSize().height;
  }

  private static class DropDownAction extends LinkLabel<Void> {
    Image myIcon = AllIcons.Ide.Notification.DropTriangle;

    public DropDownAction(String text, @Nullable LinkListener<Void> listener) {
      super(text, null, listener);

      setHorizontalTextPosition(SwingConstants.LEADING);
      setIconTextGap(0);

      setIcon(new Icon() {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
          int lineY = getUI().getBaseline(DropDownAction.this, getWidth(), getHeight()) - getIconHeight();

          Icon icon = TargetAWT.to(ImageEffects.colorize(myIcon, TargetAWT.from(getTextColor())));
          icon.paintIcon(c, g, x - 1, lineY);
        }

        @Override
        public int getIconWidth() {
          return myIcon.getWidth();
        }

        @Override
        public int getIconHeight() {
          return myIcon.getHeight();
        }
      });
    }

    @Nonnull
    @Override
    protected Rectangle getTextBounds() {
      Rectangle bounds = super.getTextBounds();
      bounds.x -= getIcon().getIconWidth();
      bounds.width += 8;
      return bounds;
    }
  }

  private static void showPopup(@Nonnull LinkLabel link, @Nonnull DefaultActionGroup group) {
    ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
    menu.getComponent().show(link, JBUI.scale(-10), link.getHeight() + JBUI.scale(2));
  }

  @Nullable
  private static Point getCollapsedTextEndLocation(@Nonnull JEditorPane text, @Nonnull BalloonLayoutData layoutData) {
    try {
      int end = text.viewToModel(new Point(10, layoutData.twoLineHeight + 5));
      if (end == -1) {
        end = text.getDocument().getLength();
      }
      for (int i = end - 1; i >= 0; i--) {
        Rectangle r = text.modelToView(i);
        if (r != null && r.y < layoutData.twoLineHeight) {
          return r.getLocation();
        }
      }
    }
    catch (BadLocationException ignored) {
    }

    return null;
  }

  private static int getFirstLineHeight(@Nonnull JEditorPane text) {
    try {
      int end = text.getDocument().getLength();
      for (int i = 0; i < end; i++) {
        Rectangle r = text.modelToView(i);
        if (r != null && r.height > 0) {
          return r.height;
        }
      }
    }
    catch (BadLocationException ignored) {
    }
    return 0;
  }

  private static class CenteredLayoutWithActions extends BorderLayout {
    private final JEditorPane myText;
    private final BalloonLayoutData myLayoutData;
    private Component myTitleComponent;
    private Component myCenteredComponent;
    private JPanel myActionPanel;
    private Component myExpandAction;

    public CenteredLayoutWithActions(JEditorPane text, BalloonLayoutData layoutData) {
      myText = text;
      myLayoutData = layoutData;
    }

    @Nullable
    public Component getTitle() {
      if (myTitleComponent != null) {
        return myTitleComponent;
      }
      if (myCenteredComponent != null) {
        return myCenteredComponent instanceof JScrollPane jScrollPane ? jScrollPane.getViewport().getView() : myCenteredComponent;
      }
      return null;
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
      if (BorderLayout.NORTH.equals(constraints)) {
        myTitleComponent = comp;
      }
      else if (BorderLayout.CENTER.equals(constraints)) {
        myCenteredComponent = comp;
      }
      else if (BorderLayout.SOUTH.equals(constraints)) {
        myActionPanel = (JPanel)comp;
      }
      else if (BorderLayout.EAST.equals(constraints)) {
        myExpandAction = comp;
      }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
      addLayoutComponent(comp, name);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return layoutSize(Component::getPreferredSize);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      return layoutSize(Component::getMinimumSize);
    }

    private Dimension layoutSize(@Nonnull Function<Component, Dimension> size) {
      Dimension titleSize = myTitleComponent == null ? new Dimension() : size.apply(myTitleComponent);
      Dimension centeredSize = myCenteredComponent == null ? new Dimension() : size.apply(myCenteredComponent);
      Dimension actionSize = myActionPanel == null ? new Dimension() : size.apply(myActionPanel);
      Dimension expandSize = myExpandAction == null || myLayoutData.showMinSize ? new Dimension() : size.apply(myExpandAction);

      int height = myLayoutData.configuration.topSpaceHeight + titleSize.height + centeredSize.height + Math.max(actionSize.height,
                                                                                                                 expandSize.height) + myLayoutData.configuration.bottomSpaceHeight;

      if (titleSize.height > 0 && centeredSize.height > 0) {
        height += myLayoutData.configuration.titleContentSpaceHeight;
      }
      if (centeredSize.height > 0 && actionSize.height > 0) {
        height += myLayoutData.configuration.contentActionsSpaceHeight;
      }
      if (titleSize.height > 0 && actionSize.height > 0) {
        height += myLayoutData.configuration.titleActionsSpaceHeight;
      }

      int titleWidth = titleSize.width + myLayoutData.configuration.closeOffset;
      int centerWidth = centeredSize.width + myLayoutData.configuration.closeOffset;
      int actionWidth = actionSize.width + expandSize.width;

      int width = Math.max(centerWidth, Math.max(titleWidth, actionWidth));
      if (!myLayoutData.showFullContent) {
        width = Math.min(width, BalloonLayoutConfiguration.MaxWidth());
      }
      width = Math.max(width, BalloonLayoutConfiguration.MinWidth());

      return new Dimension(width, height);
    }

    @Override
    public void layoutContainer(Container parent) {
      int top = myLayoutData.configuration.topSpaceHeight;
      int width = parent.getWidth();
      Dimension centeredSize = myCenteredComponent == null ? new Dimension() : myCenteredComponent.getPreferredSize();
      boolean isActions = myActionPanel != null || (myExpandAction != null && !myLayoutData.showMinSize);

      if (myTitleComponent != null) {
        int titleHeight = myTitleComponent.getPreferredSize().height;
        myTitleComponent.setBounds(0, top, width - myLayoutData.configuration.closeOffset, titleHeight);
        top += titleHeight;

        if (myCenteredComponent != null) {
          top += myLayoutData.configuration.titleContentSpaceHeight;
        }
        else if (isActions) {
          top += myLayoutData.configuration.titleActionsSpaceHeight;
        }
      }

      if (myCenteredComponent != null) {
        int centeredWidth = width;
        if (!myLayoutData.showFullContent && !myLayoutData.showMinSize && myLayoutData.fullHeight != myLayoutData.maxScrollHeight) {
          centeredWidth--;
        }
        myCenteredComponent.setBounds(0, top, centeredWidth, centeredSize.height);
        myCenteredComponent.revalidate();
      }

      if (myExpandAction != null) {
        Dimension size = myExpandAction.getPreferredSize();
        int x = width - size.width - myLayoutData.configuration.rightActionsOffset.width;

        if (myLayoutData.showMinSize) {
          Point location = getCollapsedTextEndLocation(myText, myLayoutData);
          if (location != null) {
            int y = SwingUtilities.convertPoint(myText, location.x, location.y, parent).y;
            myExpandAction.setBounds(x, y, size.width, size.height);
          }
        }
        else {
          int y = parent.getHeight() - size.height - myLayoutData.configuration.bottomSpaceHeight;
          myExpandAction.setBounds(x, y, size.width, size.height);
        }
      }

      if (myActionPanel != null) {
        int expandWidth = myExpandAction == null || myLayoutData.showMinSize ? 0 : myExpandAction.getPreferredSize().width;
        width -= myLayoutData.configuration.actionGap + expandWidth;

        int components = myActionPanel.getComponentCount();
        Component lastComponent = myActionPanel.getComponent(components - 1);
        if (lastComponent instanceof DropDownAction/* || lastComponent instanceof ContextHelpLabel*/) {
          components--;
        }
        if (components > 2) {
          myActionPanel.getComponent(0).setVisible(false);
          for (int i = 1; i < components; i++) {
            Component component = myActionPanel.getComponent(i);
            if (component.isVisible()) {
              break;
            }
            component.setVisible(true);
          }
          myActionPanel.doLayout();
          if (myActionPanel.getPreferredSize().width > width) {
            myActionPanel.getComponent(0).setVisible(true);
            myActionPanel.getComponent(1).setVisible(false);
            myActionPanel.getComponent(2).setVisible(false);
            myActionPanel.doLayout();
            for (int i = 3; i < components - 1; i++) {
              if (myActionPanel.getPreferredSize().width > width) {
                myActionPanel.getComponent(i).setVisible(false);
                myActionPanel.doLayout();
              }
              else {
                break;
              }
            }
          }
        }

        Dimension size = myActionPanel.getPreferredSize();
        int y = parent.getHeight() - size.height - myLayoutData.configuration.bottomSpaceHeight;
        myActionPanel.setBounds(0, y, width, size.height);
      }
    }
  }

  private static class LafHandler implements Runnable {
    private final JEditorPane myContent;
    private final String myContentTextR;
    private final String myContentTextD;

    private JLabel myTitle;
    private String myTitleTextR;
    private String myTitleTextD;

    public LafHandler(@Nonnull JEditorPane content, @Nonnull String textR, @Nonnull String textD) {
      myContent = content;
      myContentTextR = textR;
      myContentTextD = textD;
      updateContent();
    }

    public void setTitle(@Nonnull JLabel title, @Nonnull String textR, @Nonnull String textD) {
      myTitle = title;
      myTitleTextR = textR;
      myTitleTextD = textD;
      updateTitle();
    }

    private void updateTitle() {
      myTitle.setText(StyleManager.get().getCurrentStyle().isDark() ? myTitleTextD : myTitleTextR);
    }

    private void updateContent() {
      myContent.setText(StyleManager.get().getCurrentStyle().isDark() ? myContentTextD : myContentTextR);
    }

    @Override
    public void run() {
      if (myTitle != null) {
        updateTitle();
      }
      updateContent();
    }
  }

  private static class TextCaret extends DefaultCaret implements UIResource {
    private final BalloonLayoutData myLayoutData;

    public TextCaret(@Nonnull BalloonLayoutData layoutData) {
      myLayoutData = layoutData;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mouseClicked(e);
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mousePressed(e);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mouseReleased(e);
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mouseEntered(e);
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mouseExited(e);
      }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mouseDragged(e);
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if (!myLayoutData.showMinSize) {
        super.mouseMoved(e);
      }
    }
  }
}