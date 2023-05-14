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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.ide.tipOfDay.TipOfDayManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

@ExtensionImpl(order = "last")
public class TipOfTheDayStartupActivity implements PostStartupActivity, DumbAware {
  private final TipOfDayManager myManager;
  private final GeneralSettings myGeneralSettings;

  @Inject
  public TipOfTheDayStartupActivity(TipOfDayManager manager, GeneralSettings generalSettings) {
    myManager = manager;
    myGeneralSettings = generalSettings;
  }

  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    if (!myGeneralSettings.isShowTipsOnStartup()) {
      return;
    }

    myManager.scheduleShow(uiAccess, project);
  }
}
