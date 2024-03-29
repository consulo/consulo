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
package consulo.versionControlSystem.merge;

import consulo.ui.ex.awt.ColumnInfo;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Represents the state of a multiple file merge operation.
 *
 * @author yole
 * @see MergeProvider2#createMergeSession
 * @since 8.1
 */
public interface MergeSession {
  enum Resolution {
    Merged, AcceptedYours, AcceptedTheirs
  }

  /**
   * Returns the list of additional columns to be displayed in the dialog. The Item type for the
   * column should be VirtualFile.
   *
   * @return the list of columns, or an empty list if no additional columns should be displayed.
   */
  ColumnInfo[] getMergeInfoColumns();

  /**
   * Returns true if a merge operation can be invoked for the specified virtual file, false otherwise.
   *
   * @param file a file shown in the dialog.
   * @return true if the merge dialog can be shown, false otherwise.
   */
  boolean canMerge(VirtualFile file);

  /**
   * Called when the user executes one of the resolve actions (merge, accept yours, accept theirs) for
   * a conflicting file. Note that if canMerge() is false for the file, the nothing is done by the idea,
   * the implementer should perform necessary actions for conflict resolution itself.
   *
   * @param file       the conflicting file.
   * @param resolution the used resolution.
   */
  void conflictResolvedForFile(VirtualFile file, Resolution resolution);

}
