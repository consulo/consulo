/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.project.event;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.project.Project;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;
import java.util.EventListener;

/**
 * Listener for Project.
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface ProjectManagerListener extends EventListener {
  ProjectManagerListener[] EMPTY_ARRAY = new ProjectManagerListener[0];

  /**
   * Invoked on project open.
   *
   * @param project opening project
   */
  default void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    projectOpened(project);
  }

  /**
   * Invoked on project open.
   *
   * @param project opening project
   */
  @Deprecated
  @DeprecationInfo("Use projectOpened(Project, UIAccess)")
  default void projectOpened(@Nonnull Project project) {
  }

  /**
   * Invoked on project close.
   *
   * @param project closing project
   */
  default void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    projectClosed(project);
  }

  /**
   * Invoked on project close.
   *
   * @param project closing project
   */
  @Deprecated
  @DeprecationInfo("Use projectClosed(Project, UIAccess)")
  default void projectClosed(@Nonnull Project project) {
  }

  /**
   * Invoked on project close before any closing activities
   */
  default void projectClosing(@Nonnull Project project) {
  }
}
