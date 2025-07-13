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
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Lobas
 */
public final class SettingsEntryPointAction extends DumbAwareActionGroup implements RightAlignedToolbarAction {
    public enum IconState {
        Default(PlatformIconGroup.generalGearplain()),
        ApplicationUpdate(PlatformIconGroup.ideNotificationIdeupdate()),
        ApplicationComponentUpdate(PlatformIconGroup.ideNotificationPluginupdate()),
        RestartRequired(PlatformIconGroup.ideNotificationRestartrequiredupdate());

        private final Image myIcon;

        IconState(Image icon) {
            myIcon = icon;
        }
    }

    private final Application myApplication;
    private final Provider<UpdateSettings> myUpdateSettingsProvider;

    @Inject
    public SettingsEntryPointAction(Application application,
                                    Provider<UpdateSettings> updateSettingsProvider) {
        super(IdeLocalize.settingsEntryPointTooltip(), IdeLocalize.settingsEntryPointTooltip(), PlatformIconGroup.generalGearplain());
        myApplication = application;
        myUpdateSettingsProvider = updateSettingsProvider;
        setPopup(true);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> groups = new ArrayList<>();

        myApplication.getExtensionPoint(SettingsEntryPointActionProvider.class).forEachExtensionSafe(provider -> {
            groups.add(provider.getUpdateActionOrGroup());
        });

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
        IconState state = getState((UpdateSettingsEx) myUpdateSettingsProvider.get());
        presentation.setIcon(state.myIcon);
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }

    @RequiredUIAccess
    public static IconState getState(UpdateSettingsEx updateSettingsEx) {
        PlatformOrPluginUpdateResultType lastCheckResult = updateSettingsEx.getLastCheckResult();

        switch (lastCheckResult) {
            case PLATFORM_UPDATE:
                return IconState.ApplicationUpdate;
            case RESTART_REQUIRED:
                return IconState.RestartRequired;
            case PLUGIN_UPDATE:
                return IconState.ApplicationComponentUpdate;
            default:
                return IconState.Default;
        }
    }
}