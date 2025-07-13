// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.internal.UpdateSettingsEx;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.SettingsEntryPointActionProvider;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Lobas
 */
public final class SettingsEntryPointAction extends DumbAwareActionGroup implements RightAlignedToolbarAction {
    public enum IconState {
        Default,
        ApplicationUpdate,
        ApplicationComponentUpdate,
        RestartRequired
    }

    private static IconState ourIconState = IconState.Default;

    public SettingsEntryPointAction() {
        super(IdeLocalize.settingsEntryPointTooltip(), IdeLocalize.settingsEntryPointTooltip(), PlatformIconGroup.generalGearplain());
        setPopup(true);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> groups = new ArrayList<>();

        Application.get().getExtensionPoint(SettingsEntryPointActionProvider.class).forEachExtensionSafe(provider -> {
            groups.add(provider.getUpdateActionOrGroup());
        });

        if (groups.isEmpty()) {
            resetActionIcon();
        }

        ActionGroup templateGroup = (ActionGroup) ActionManager.getInstance().getAction("SettingsEntryPointGroup");
        if (templateGroup != null) {
            groups.add(templateGroup);
        }

        return groups.toArray(AnAction.EMPTY_ARRAY);
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(getActionIcon(ourIconState));
        updateState(UpdateSettings.getInstance());
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }

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
}