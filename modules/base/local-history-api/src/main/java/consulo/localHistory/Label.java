/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.localHistory;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public interface Label {
  Label NULL_INSTANCE = new Label() {

    @Override
    public void revert(@Nonnull Project project, @Nonnull VirtualFile file) {
    }

    @Override
    public ByteContent getByteContent(String path) {
      return null;
    }
  };

  /**
   * Revert all changes up to this Label according to the local history
   *
   * @param file file or directory that should be reverted
   * @throws LocalHistoryException
   */
  void revert(@Nonnull Project project, @Nonnull VirtualFile file) throws LocalHistoryException;

  ByteContent getByteContent(String path);
}
