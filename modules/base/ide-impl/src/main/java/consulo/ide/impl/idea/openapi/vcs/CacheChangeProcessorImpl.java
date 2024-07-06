/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.openapi.vcs;

import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 06-Jul-24
 */
public interface CacheChangeProcessorImpl {
  String getPlace();

  default void init(Project project, CacheChangeProcessorBridge bridge) {
  }

  List<Change> getSelectedChanges();

  List<Change> getAllChanges();

  void selectChange(@Nonnull Change change);

  default void onAfterNavigate() {
  }

  default Boolean isWindowFocused() {
    return null;
  }
}
