/*
 * Copyright 2013-2022 consulo.io
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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesViewContentManager;
import consulo.project.Project;
import consulo.versionControlSystem.VcsInitObject;
import consulo.versionControlSystem.VcsStartupActivity;
import jakarta.inject.Inject;

@ExtensionImpl
public class UpdateToolWindowVcsStartupActivity implements VcsStartupActivity {
  private final Project myProject;

  @Inject
  public UpdateToolWindowVcsStartupActivity(Project project) {
    myProject = project;
  }

  @Override
  public void runActivity() {
    ChangesViewContentManager manager = (ChangesViewContentManager)ChangesViewContentManager.getInstance(myProject);

    manager.update();
  }

  @Override
  public int getOrder() {
    return VcsInitObject.AFTER_COMMON.getOrder();
  }
}
