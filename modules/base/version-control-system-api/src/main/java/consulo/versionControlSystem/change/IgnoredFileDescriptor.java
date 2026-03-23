/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

public interface IgnoredFileDescriptor {
  /**
   * Path to file or directory in case if {@link IgnoreSettingsType#FILE} or {@link IgnoreSettingsType#UNDER_DIR}
   *
   * @return full path to file or directory. null in case if {@link IgnoreSettingsType} is {@link IgnoreSettingsType#MASK}
   */
  @Nullable
  String getPath();

  /**
   * Ignored mask represents ignore pattern in ignore files for different VCS (e.g. .gitignore, .hgignore, etc).
   * The recommended way to support all VCS is to choose a common pattern for mask.
   * Note: At the moment, this mask will be written to ignore file without any pre-processing (as it is)
   *
   * @return ignored mask. null in case if {@link IgnoreSettingsType} is not {@link IgnoreSettingsType#MASK}
   */
  @Nullable
  String getMask();

  IgnoreSettingsType getType();

  /**
   * @deprecated use {@link #matchesFile(FilePath)}
   */
  @Deprecated
  boolean matchesFile(VirtualFile file);

  boolean matchesFile(FilePath filePath);
}
