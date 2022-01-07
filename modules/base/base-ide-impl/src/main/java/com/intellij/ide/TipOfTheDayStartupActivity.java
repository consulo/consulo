/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import consulo.ide.tipOfDay.TipOfDayManager;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

public class TipOfTheDayStartupActivity implements StartupActivity.DumbAware {
  private final TipOfDayManager myManager;
  private final GeneralSettings myGeneralSettings;

  @Inject
  public TipOfTheDayStartupActivity(TipOfDayManager manager, GeneralSettings generalSettings) {
    myManager = manager;
    myGeneralSettings = generalSettings;
  }

  @Override
  public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    if (!myGeneralSettings.isShowTipsOnStartup()) {
      return;
    }

    myManager.scheduleShow(uiAccess, project);
  }
}
