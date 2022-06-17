/*
 * Copyright 2013-2018 consulo.io
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
package consulo.project.ui.wm.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
@Service(ComponentScope.PROJECT)
public interface ProjectIdeFocusManager extends IdeFocusManager {
  @Nonnull
  static IdeFocusManager getInstance(@Nullable Project project) {
    if (project == null || project.isDisposed() || !project.isInitialized()) return ApplicationIdeFocusManager.getInstance();

    return project.getComponent(ProjectIdeFocusManager.class);
  }
}
