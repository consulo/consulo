// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.internal.UpdateSettingsEx;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.AnActionButton;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.internal.SettingsEntryPointActionProvider;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Alexander Lobas
 */
public final class SettingsEntryPointAction extends DumbAwareAction implements RightAlignedToolbarAction {
    private boolean myShowPopup = true;

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        // why? resetActionIcon();

        if (myShowPopup) {
            myShowPopup = false;
            ListPopup popup = createMainPopup(e.getDataContext(), () -> myShowPopup = true);
            PopupUtil.showForActionButtonEvent(popup, e);
        }
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setTextValue(LocalizeValue.empty());
        presentation.setDescriptionValue(getActionTooltip());
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
                        @RequiredUIAccess
                        public void updateButton(@Nonnull AnActionEvent e) {
                            getDelegate().update(e);
                            e.getPresentation().setText(e.getPresentation().getText() + "...");
                        }

                        @Override
                        public void actionPerformed(@Nonnull AnActionEvent e) {
                            super.actionPerformed(new AnActionEvent(
                                e.getInputEvent(),
                                e.getDataContext(),
                                e.getPlace(),
                                getDelegate().getTemplatePresentation(),
                                e.getActionManager(),
                                e.getModifiers()
                            ));
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

        return JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            group.build(),
            context,
            JBPopupFactory.ActionSelectionAid.MNEMONICS,
            true,
            () -> AppExecutorUtil.getAppScheduledExecutorService().schedule(
                () -> Application.get().invokeLater(disposeCallback, Application.get().getAnyModalityState()),
                250,
                TimeUnit.MILLISECONDS
            ),
            -1
        );
    }

    private static IconState ourIconState = IconState.Default;

    @RequiredUIAccess
    public static void updateState(UpdateSettings updateSettings) {
        UpdateSettingsEx updateSettingsEx = (UpdateSettingsEx)updateSettings;

        PlatformOrPluginUpdateResultType lastCheckResult = updateSettingsEx.getLastCheckResult();

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

    @RequiredUIAccess
    public static void updateState(IconState state) {
        ourIconState = state;

        if (isAvailableInStatusBar()) {
            updateWidgets();
        }
    }

    @Nonnull
    private static LocalizeValue getActionTooltip() {
        return IdeLocalize.settingsEntryPointTooltip();
    }

    private static void resetActionIcon() {
        ourIconState = IconState.Default;
    }

    @Nonnull
    private static Image getActionIcon(IconState iconState) {
        switch (iconState) {
            case ApplicationUpdate:
                return PlatformIconGroup.ideNotificationIdeupdate();
            case ApplicationComponentUpdate:
                return PlatformIconGroup.ideNotificationPluginupdate();
            case RestartRequired:
                return PlatformIconGroup.ideNotificationRestartrequiredupdate();
        }
        return PlatformIconGroup.generalGearplain();
    }

    private static UISettingsListener mySettingsListener;

    private static void initUISettingsListener() {
        if (mySettingsListener == null) {
            mySettingsListener = uiSettings -> updateWidgets();
            Application.get().getMessageBus().connect().subscribe(UISettingsListener.class, mySettingsListener);
        }
    }

    @RequiredUIAccess
    private static void updateWidgets() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            project.getInstance(StatusBarWidgetsManager.class).updateWidget(StatusBarManager.class, UIAccess.current());
            IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
            if (frame != null) {
                StatusBar statusBar = frame.getStatusBar();
                if (statusBar != null) {
                    statusBar.updateWidget(it -> it instanceof MyStatusBarWidget);
                }
            }
        }
    }

    private static boolean isAvailableInStatusBar() {
        initUISettingsListener();
        // TODO [VISTALL] it's disabled by default in idea, enabled by experimental settings
        return false;
    }

    public static class StatusBarManager implements StatusBarWidgetFactory {
        @Nls
        @Nonnull
        @Override
        public String getDisplayName() {
            return IdeLocalize.settingsEntryPointWidgetName().get();
        }

        @Override
        public boolean isAvailable(@Nonnull Project project) {
            return isAvailableInStatusBar();
        }

        @Nonnull
        @Override
        public StatusBarWidget createWidget(@Nonnull Project project) {
            return new MyStatusBarWidget(this);
        }

        @Override
        public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
            return isAvailableInStatusBar();
        }
    }

    private static class MyStatusBarWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
        private final StatusBarWidgetFactory myFactory;
        private StatusBar myStatusBar;
        private boolean myShowPopup = true;

        public MyStatusBarWidget(StatusBarWidgetFactory factory) {
            myFactory = factory;
        }

        @Nonnull
        @Override
        public String getId() {
            return myFactory.getId();
        }

        @Override
        public void install(@Nonnull StatusBar statusBar) {
            myStatusBar = statusBar;
        }

        @Nullable
        @Override
        public WidgetPresentation getPresentation() {
            return this;
        }

        @Nullable
        @Override
        public String getTooltipText() {
            return getActionTooltip().get();
        }

        @Nullable
        @Override
        public Consumer<MouseEvent> getClickConsumer() {
            return event -> {
                // why? resetActionIcon();
                myStatusBar.updateWidget(it -> it instanceof MyStatusBarWidget);

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

        @Nullable
        @Override
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