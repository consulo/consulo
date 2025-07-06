/*
 * Copyright 2013-2021 consulo.io
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
package consulo.externalService.impl.internal.update;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.internal.UpdateSettingsEx;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.update.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.SettingsEntryPointActionProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2021-04-01
 */
@ExtensionImpl
public class UpdateSettingsEntryPointActionProvider implements SettingsEntryPointActionProvider {
    private static class IconifiedCheckForUpdateAction extends CheckForUpdateAction {
        private final boolean myIsPlatform;

        public IconifiedCheckForUpdateAction(Provider<UpdateSettings> updateSettingsProvider, boolean isPlatform) {
            super(updateSettingsProvider);
            getTemplatePresentation().setTextValue(
                isPlatform
                    ? ExternalServiceLocalize.actionUpdatePlatformAndPluginsText(Application.get().getName())
                    : ExternalServiceLocalize.actionUpdatePluginsText()
            );
            myIsPlatform = isPlatform;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();

            presentation.setEnabledAndVisible(true);
            presentation.setDisabledMnemonic(true);
            presentation.setIcon(myIsPlatform ? PlatformIconGroup.ideNotificationIdeupdate() : PlatformIconGroup.ideNotificationPluginupdate());
        }
    }

    private static class RestartConsuloAction extends DumbAwareAction {
        private final UpdateSettings myUpdateSettings;

        private RestartConsuloAction(UpdateSettings updateSettings) {
            super(
                ExternalServiceLocalize.actionRestartAppText(Application.get().getName()),
                LocalizeValue.empty(),
                PlatformIconGroup.ideNotificationRestartrequiredupdate()
            );
            myUpdateSettings = updateSettings;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Application application = Application.get();
            application.restart();
            ((UpdateSettingsEx) myUpdateSettings).setLastCheckResult(PlatformOrPluginUpdateResultType.RESTART_REQUIRED);
        }
    }

    private final Provider<ActionManager> myActionManagerProvider;
    private final Provider<UpdateSettings> myUpdateSettingsProvider;

    @Inject
    public UpdateSettingsEntryPointActionProvider(
        Provider<ActionManager> actionManagerProvider,
        Provider<UpdateSettings> updateSettingsProvider
    ) {
        myActionManagerProvider = actionManagerProvider;
        myUpdateSettingsProvider = updateSettingsProvider;
    }

    @Nonnull
    @Override
    public Collection<AnAction> getUpdateActions(@Nonnull DataContext context) {
        UpdateSettings updateSettings = myUpdateSettingsProvider.get();

        PlatformOrPluginUpdateResultType type = ((UpdateSettingsEx) updateSettings).getLastCheckResult();
        return switch (type) {
            case NO_UPDATE -> List.of(myActionManagerProvider.get().getAction("CheckForUpdate"));
            case RESTART_REQUIRED -> List.<AnAction>of(new RestartConsuloAction(updateSettings));
            case PLATFORM_UPDATE, PLUGIN_UPDATE -> List.<AnAction>of(new IconifiedCheckForUpdateAction(
                myUpdateSettingsProvider,
                type == PlatformOrPluginUpdateResultType.PLATFORM_UPDATE
            ));
            default -> List.<AnAction>of();
        };
    }
}
