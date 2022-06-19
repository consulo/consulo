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
package consulo.language.editor;

import consulo.annotation.component.Topic;
import consulo.fileEditor.FileEditor;

import javax.annotation.Nonnull;
import java.util.Collection;

@Topic
public interface DaemonListener {
  /**
   * Fired when the background code analysis is being scheduled for the specified set of files.
   *
   * @param fileEditors The list of files that will be analyzed during the current execution of the daemon.
   */
  default void daemonStarting(@Nonnull Collection<FileEditor> fileEditors) {
  }

  /**
   * Fired when the background code analysis is done.
   */
  default void daemonFinished() {
  }

  /**
   * Fired when the background code analysis is done.
   *
   * @param fileEditors The list of files analyzed during the current execution of the daemon.
   */
  default void daemonFinished(@Nonnull Collection<FileEditor> fileEditors) {
    daemonFinished();
  }

  default void daemonCancelEventOccurred(@Nonnull String reason) {
  }
}
