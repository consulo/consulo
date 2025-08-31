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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@TopicImpl(ComponentScope.APPLICATION)
final class VcsShutDownProjectListener implements ProjectManagerListener {
  @Inject
  VcsShutDownProjectListener() {
  }

  @Override
  public void projectClosing(@Nonnull Project project) {
    if (project.isDefault()) return;
    VcsInitialization vcsInitialization = project.getInstanceIfCreated(VcsInitialization.class);
    if (vcsInitialization != null) {
      // Wait for the task to terminate, to avoid running it in background for closed project
      vcsInitialization.cancelBackgroundInitialization();
    }
  }
}
