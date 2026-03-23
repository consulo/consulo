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
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.versionControlSystem.FilePath;

import java.util.Collections;
import java.util.Set;

@ExtensionAPI(ComponentScope.PROJECT)
public interface IgnoredFileProvider {
  boolean isIgnoredFilePath(FilePath filePath);

  /**
   * Returns the set of files/masks/directories that this provider wants to add to VCS ignore files.
   */
  default Set<IgnoredFileDescriptor> getIgnoredFiles() {
    return Collections.emptySet();
  }

  /**
   * Returns a human-readable description of the group of ignored files provided by this extension.
   */
  default String getIgnoredGroupDescription() {
    return "";
  }
}