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
package consulo.versionControlSystem.distributed.repository;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.vcs.VcsInitObject;
import consulo.vcs.VcsStartupActivity;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

@ExtensionImpl
final class VcsRepositoryManagerStartupActivity implements VcsStartupActivity {
  @Inject
  VcsRepositoryManagerStartupActivity() {
  }

  @Override
  public void runActivity(@Nonnull Project project) {
    VcsRepositoryManager.getInstance(project).checkAndUpdateRepositoriesCollection(null);
  }

  @Override
  public int getOrder() {
    return VcsInitObject.OTHER_INITIALIZATION.getOrder();
  }
}
