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
package consulo.versionControlSystem.annotate;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsProviderMarker;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

public interface AnnotationProvider extends VcsProviderMarker {
  @Nonnull
  FileAnnotation annotate(@Nonnull VirtualFile file) throws VcsException;

  @Nonnull
  FileAnnotation annotate(@Nonnull VirtualFile file, VcsFileRevision revision) throws VcsException;

  /**
   * Check whether the annotation retrieval is valid (or possible) for the
   * particular file revision (or version in the repository).
   * @param rev File revision to be checked.
   * @return true if annotation it valid for the given revision.
   */
  default boolean isAnnotationValid(@Nonnull VcsFileRevision rev) { return true; }

  default boolean isCaching() { return false; }

  @Nonnull
  default LocalizeValue getActionName() {
    return ActionLocalize.actionAnnotateText();
  }
}
