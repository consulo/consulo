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
package consulo.project.startup;

import consulo.annotation.DeprecationInfo;
import consulo.project.Project;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;

/**
 * <p>Runs an activity on project open.</p>
 *
 * <p>If the activity implements {@link consulo.application.dumb.DumbAware} interface, e.g. {@link DumbAware}, it will be started in a pooled thread
 * under 'Loading Project' dialog, otherwise it will be started in the dispatch thread after the initialization.</p>
 *
 * @author Dmitry Avdeev
 */
public interface StartupActivity {
  @Deprecated
  @DeprecationInfo("Use consulo.application.dumb.DumbAware")
  interface DumbAware extends StartupActivity, consulo.application.dumb.DumbAware {
  }

  void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess);
}
