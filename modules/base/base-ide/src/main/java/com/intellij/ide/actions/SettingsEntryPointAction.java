// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.intellij.ui.AnActionButton;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.disposer.Disposer;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateResult;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Lobas
 */
public final class SettingsEntryPointAction extends DumbAwareAction implements RightAlignedToolbarAction, TooltipDescriptionProvider {
  private boolean myShowPopup = true;

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    // why? resetActionIcon();

    if (myShowPopup) {
      myShowPopup = false;
      ListPopup popup = createMainPopup(e.getDataContext(), () -> myShowPopup = true);
      PopupUtil.showForActionButtonEvent(popup, e);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setTextValue(LocalizeValue.of());
    presentation.setDescription(getActionTooltip());
    presentation.setIcon(getActionIcon(ourIconState));

    updateState(UpdateSettings.getInstance());

    for (AnAction child : getTemplateActions()) {
      child.update(AnActionEvent.createFromAnAction(this, e.getInputEvent(), e.getPlace(), e.getDataContext()));
    }
  }

  @Nonnull
  private static AnAction[] getTemplateActions() {
    ActionGroup templateGroup = (ActionGroup)ActionManager.getInstance().getAction("SettingsEntryPointGroup");
    return templateGroup == null ? EMPTY_ARRAY : templateGroup.getChildren(null);
  }

  @Nonnull
  private static ListPopup createMainPopup(@Nonnull DataContext context, @Nonnull Runnable disposeCallback) {
    ActionGroup.Builder group = ActionGroup.newImmutableBuilder();

    for (SettingsEntryPointActionProvider provider : SettingsEntryPointActionProvider.EP_NAME.getExtensionList()) {
      Collection<AnAction> actions = provider.getUpdateActions(context);
      if (!actions.isEmpty()) {
        for (AnAction action : actions) {
          Presentation presentation = action.getTemplatePresentation();
          IconState iconState = (IconState)presentation.getClientProperty(SettingsEntryPointActionProvider.ICON_KEY);
          if (iconState != null) {
            presentation.setIcon(getActionIcon(iconState));
          }
          group.add(action);
        }
        group.addSeparator();
      }
    }

    if (group.isEmpty()) {
      resetActionIcon();
    }

    for (AnAction child : getTemplateActions()) {
      if (child instanceof AnSeparator) {
        group.add(child);
      }
      else {
        String text = child.getTemplateText();
        if (text != null && !text.endsWith("...")) {
          AnActionButton button = new AnActionButton.AnActionButtonWrapper(child.getTemplatePresentation(), child) {
            @Override
            public void updateButton(@Nonnull AnActionEvent e) {
              getDelegate().update(e);
              e.getPresentation().setText(e.getPresentation().getText() + "...");
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
              super.actionPerformed(new AnActionEvent(e.getInputEvent(), e.getDataContext(), e.getPlace(), getDelegate().getTemplatePresentation(), e.getActionManager(), e.getModifiers()));
            }
          };
          button.setShortcut(child.getShortcutSet());
          group.add(button);
        }
        else {
          group.add(child);
        }
      }
    }

    return JBPopupFactory.getInstance().createActionGroupPopup(null, group.build(), context, JBPopupFactory.ActionSelectionAid.MNEMONICS, true, () -> {
      AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> ApplicationManager.getApplication().invokeLater(disposeCallback, ModalityState.any()), 250, TimeUnit.MILLISECONDS);
    }, -1);
  }

  private static IconState ourIconState = IconState.Default;

  public static void updateState(UpdateSettings updateSettings) {
    PlatformOrPluginUpdateResult.Type lastCheckResult = updateSettings.getLastCheckResult();

    switch (lastCheckResult) {
      case PLATFORM_UPDATE:
        updateState(IconState.ApplicationUpdate);
        break;
      case RESTART_REQUIRED:
        updateState(IconState.RestartRequired);
        break;
      case PLUGIN_UPDATE:
        updateState(IconState.ApplicationComponentUpdate);
        break;
      default:
        resetActionIcon();
        break;
    }
  }

  public static void updateState(IconState state) {
    ourIconState = state;

    if (isAvailableInStatusBar()) {
      updateWidgets();
    }
  }

  @Nonnull
  @Nls
  private static String getActionTooltip() {
    return IdeBundle.message("settings.entry.point.tooltip");
  }

  private static void resetActionIcon() {
    ourIconState = IconState.Default;
  }

  @Nonnull
  private static Image getActionIcon(IconState iconState) {
    switch (iconState) {
      case ApplicationUpdate:
        return PlatformIconGroup.ideNotificationIdeUpdate();
      case ApplicationComponentUpdate:
        return PlatformIconGroup.ideNotificationPluginUpdate();
      case RestartRequired:
        return PlatformIconGroup.ideNotificationRestartRequiredUpdate();
    }
    return AllIcons.General.GearPlain;
  }

  private static UISettingsListener mySettingsListener;

  private static void initUISettingsListener() {
    if (mySettingsListener == null) {
      mySettingsListener = uiSettings -> updateWidgets();
      ApplicationManager.getApplication().getMessageBus().connect().subscribe(UISettingsListener.TOPIC, mySettingsListener);
    }
  }

  private static void updateWidgets() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      project.getInstance(StatusBarWidgetsManager.class).updateWidget(StatusBarManager.class, UIAccess.current());
      IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
      if (frame != null) {
        StatusBar statusBar = frame.getStatusBar();
        if (statusBar != null) {
          statusBar.updateWidget(WIDGET_ID);
        }
      }
    }
  }

  private static boolean isAvailableInStatusBar() {
    initUISettingsListener();
    // TODO [VISTALL] it's disabled by default in idea, enabled by experimental settings
    return false;
  }

  private static final String WIDGET_ID = "settingsEntryPointWidget";

  public static class StatusBarManager implements StatusBarWidgetFactory {
    @Override
    public
    @Nonnull
    String getId() {
      return WIDGET_ID;
    }

    @Override
    @Nls
    @Nonnull
    public String getDisplayName() {
      return IdeBundle.message("settings.entry.point.widget.name");
    }

    @Override
    public boolean isAvailable(@Nonnull Project project) {
      return isAvailableInStatusBar();
    }

    @Override
    @Nonnull
    public StatusBarWidget createWidget(@Nonnull Project project) {
      return new MyStatusBarWidget();
    }

    @Override
    public void disposeWidget(@Nonnull StatusBarWidget widget) {
      Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
      return isAvailableInStatusBar();
    }
  }

  private static class MyStatusBarWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
    private StatusBar myStatusBar;
    private boolean myShowPopup = true;

    @Override
    public
    @Nonnull
    String ID() {
      return WIDGET_ID;
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
      myStatusBar = statusBar;
    }

    @Override
    @Nullable
    public WidgetPresentation getPresentation() {
      return this;
    }

    @Override
    @Nullable
    public String getTooltipText() {
      return getActionTooltip();
    }

    @Override
    @Nullable
    public Consumer<MouseEvent> getClickConsumer() {
      return event -> {
        // why? resetActionIcon();
        myStatusBar.updateWidget(WIDGET_ID);

        if (!myShowPopup) {
          return;
        }
        myShowPopup = false;

        Component component = event.getComponent();
        ListPopup popup = createMainPopup(DataManager.getInstance().getDataContext(component), () -> myShowPopup = true);
        popup.addListener(new JBPopupListener() {
          @Override
          public void beforeShown(@Nonnull LightweightWindowEvent event) {
            Point location = component.getLocationOnScreen();
            Dimension size = popup.getSize();
            popup.setLocation(new Point(location.x + component.getWidth() - size.width, location.y - size.height));
          }
        });
        popup.show(component);
      };
    }

    @Override
    @Nullable
    public Image getIcon() {
      return getActionIcon(ourIconState);
    }

    @Override
    public void dispose() {
    }
  }

  public enum IconState {
    Default,
    ApplicationUpdate,
    ApplicationComponentUpdate,
    RestartRequired
  }
}