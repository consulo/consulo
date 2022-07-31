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
package consulo.ide.impl.vcs;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.vcs.VcsInitObject;
import consulo.vcs.VcsStartupActivity;
import consulo.project.Project;

import javax.annotation.Nonnull;

@ExtensionImpl
public class UpdateToolWindowVcsStartupActivity implements VcsStartupActivity {
  @Override
  public void runActivity(@Nonnull Project project) {
    ChangesViewContentManager manager = (ChangesViewContentManager)ChangesViewContentManager.getInstance(project);

    manager.update();
  }

  @Override
  public int getOrder() {
    return VcsInitObject.AFTER_COMMON.getOrder();
  }
}
