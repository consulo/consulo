/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.annotation.component.ActionImpl;
import consulo.application.progress.Task;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.update.UpdateSettings;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ActionImpl(id = "CheckForUpdate")
public class CheckForUpdateAction extends DumbAwareAction {
  private final Provider<UpdateSettings> myUpdateSettingsProvider;

  @Inject
  public CheckForUpdateAction(Provider<UpdateSettings> updateSettingsProvider) {
    myUpdateSettingsProvider = updateSettingsProvider;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    String place = e.getPlace();
    if (ActionPlaces.WELCOME_SCREEN.equals(place) || "SettingsEntryPointGroup".equals(place)) {
      e.getPresentation().setEnabledAndVisible(true);
    }
    else {
      e.getPresentation().setVisible(!Platform.current().os().isEnabledTopMenu() || !ActionPlaces.MAIN_MENU.equals(place));
    }
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);

    actionPerformed(project, myUpdateSettingsProvider.get(), UIAccess.current());
  }

  public static void actionPerformed(Project project, UpdateSettings updateSettings, UIAccess uiAccess) {
    Task.Backgroundable.queue(project, "Checking for updates", true, indicator -> {
      indicator.setIndeterminate(true);

      AsyncResult<PlatformOrPluginUpdateResultType> result = AsyncResult.undefined();
      result.doWhenDone(() -> ((UpdateSettingsImpl)updateSettings).setLastTimeCheck(System.currentTimeMillis()));

      PlatformOrPluginUpdateChecker.checkAndNotifyForUpdates(project, true, indicator, uiAccess, result);
    });
  }
}
