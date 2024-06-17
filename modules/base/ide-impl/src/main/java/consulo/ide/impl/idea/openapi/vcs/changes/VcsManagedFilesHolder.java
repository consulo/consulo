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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.component.extension.ExtensionPointName;

/**
 * Allows to decouple parts of {@link ChangeListManager} refresh into a separate process.
 * <p>
 * When specified, {@link ChangeProvider} is no longer expected to report ignored/unversioned files.
 * Instead, these should be loaded by this handler autonomously, and updates notified using {@link VcsManagedFilesHolderListener}.
 * Note, that holder will need to implement its own logic for tracking modifications,
 * as {@link VcsDirtyScopeManager} serves only {@link ChangeProvider}.
 * <p>
 * This allows to increase general responsiveness of {@link ChangeListManager}
 * if refresh of ignored/untracked files is a dramatically slower operation than refresh of modified files.
 *
 * @see ChangeListManagerImpl#notifyUnchangedFileStatusChanged
 */
public interface VcsManagedFilesHolder extends FilePathHolder {
  ExtensionPointName<VcsUnversionedFilesHolderProvider> VCS_UNVERSIONED_FILES_HOLDER_EP =
    ExtensionPointName.create(VcsUnversionedFilesHolderProvider.class);

  ExtensionPointName<VcsIgnoredFilesHolderProvider> VCS_IGNORED_FILES_HOLDER_EP
    = new ExtensionPointName<>(VcsIgnoredFilesHolderProvider.class);

  Class<VcsManagedFilesHolderListener> TOPIC = VcsManagedFilesHolderListener.class;

  /**
   * Whether data is dirty and there's an ongoing refresh.
   * <p>
   * Ex: it affects argument passed to {@link ChangeListListener#unchangedFileStatusChanged(boolean)}
   * and progress indicator in the tree.
   */
  default boolean isInUpdatingMode() {
    return false;
  }

}
