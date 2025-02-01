/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.actions;

import consulo.externalService.plugin.PluginContants;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ShowPluginManagerAction extends DumbAwareAction {
  private final Provider<ShowSettingsUtil> myShowSettingsUtilProvider;

  @Inject
  public ShowPluginManagerAction(Provider<ShowSettingsUtil> showSettingsUtilProvider) {
    myShowSettingsUtilProvider = showSettingsUtilProvider;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    myShowSettingsUtilProvider.get().showSettingsDialog(e.getData(Project.KEY), PluginContants.CONFIGURABLE_ID, null);
  }
}
