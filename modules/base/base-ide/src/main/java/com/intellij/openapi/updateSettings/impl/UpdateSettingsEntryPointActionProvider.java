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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.actions.SettingsEntryPointActionProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.DumbAwareAction;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateResult;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 01/04/2021
 */
public class UpdateSettingsEntryPointActionProvider implements SettingsEntryPointActionProvider {
  private static class IconifiedCheckForUpdateAction extends CheckForUpdateAction {
    private final boolean myIsPlatform;

    public IconifiedCheckForUpdateAction(Provider<UpdateSettings> updateSettingsProvider, boolean isPlatform) {
      super(updateSettingsProvider);
      getTemplatePresentation().setTextValue(isPlatform ? LocalizeValue.localizeTODO("Update " + Application.get().getName() + " and Plugins") : LocalizeValue.localizeTODO("Update Plugins"));
      myIsPlatform = isPlatform;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();

      presentation.setEnabledAndVisible(true);
      presentation.setDisabledMnemonic(true);
      presentation.setIcon(myIsPlatform ? PlatformIconGroup.ideNotificationIdeUpdate() : PlatformIconGroup.ideNotificationPluginUpdate());
    }
  }

  private static class RestartConsuloAction extends DumbAwareAction {
    private final UpdateSettings myUpdateSettings;

    private RestartConsuloAction(UpdateSettings updateSettings) {
      super(LocalizeValue.localizeTODO("Restart " + Application.get().getName()), LocalizeValue.empty(), PlatformIconGroup.ideNotificationRestartRequiredUpdate());
      myUpdateSettings = updateSettings;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Application application = Application.get();
      application.restart();
      myUpdateSettings.setLastCheckResult(PlatformOrPluginUpdateResult.Type.RESTART_REQUIRED);
    }
  }

  private final Provider<ActionManager> myActionManagerProvider;
  private final Provider<UpdateSettings> myUpdateSettingsProvider;

  @Inject
  public UpdateSettingsEntryPointActionProvider(Provider<ActionManager> actionManagerProvider, Provider<UpdateSettings> updateSettingsProvider) {
    myActionManagerProvider = actionManagerProvider;
    myUpdateSettingsProvider = updateSettingsProvider;
  }

  @Nonnull
  @Override
  public Collection<AnAction> getUpdateActions(@Nonnull DataContext context) {
    UpdateSettings updateSettings = myUpdateSettingsProvider.get();

    PlatformOrPluginUpdateResult.Type type = updateSettings.getLastCheckResult();
    switch (type) {
      case NO_UPDATE:
        return List.of(myActionManagerProvider.get().getAction("CheckForUpdate"));
      case RESTART_REQUIRED:
        return List.of(new RestartConsuloAction(updateSettings));
      case PLATFORM_UPDATE:
      case PLUGIN_UPDATE:
        return List.of(new IconifiedCheckForUpdateAction(myUpdateSettingsProvider, type == PlatformOrPluginUpdateResult.Type.PLATFORM_UPDATE));
    }
    return List.of();
  }
}
