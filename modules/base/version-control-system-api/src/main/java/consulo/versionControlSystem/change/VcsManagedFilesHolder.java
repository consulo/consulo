/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

/**
 * Allows to decouple parts of ChangeListManager refresh into a separate process.
 * <p>
 * When specified, ChangeProvider is no longer expected to report ignored/unversioned files.
 * Instead, these should be loaded by this handler autonomously, and updates notified using
 * {@link VcsManagedFilesHolderListener}.
 * <p>
 * This allows to increase general responsiveness of ChangeListManager
 * if refresh of ignored/untracked files is a dramatically slower operation than refresh of modified files.
 */
public interface VcsManagedFilesHolder extends FilePathHolder {
  /**
   * Whether data is dirty and there's an ongoing refresh.
   */
  default boolean isInUpdatingMode() {
    return false;
  }
}
