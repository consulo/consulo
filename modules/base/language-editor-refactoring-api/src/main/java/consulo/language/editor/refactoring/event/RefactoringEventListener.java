/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Listener to get high level notifications about performed refactorings in the selected project.
 *
 * RefactoringEventData depends on the refactoring performed. It should reflect state of the data before/after refactoring.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface RefactoringEventListener {

  /**
   * Is fired when refactoring enters its write phase (find usages, conflict detection phases are passed already)
   */
  void refactoringStarted(@Nonnull String refactoringId, @Nullable RefactoringEventData beforeData);

  /**
   * Is fired when refactoring is completed, probably with conflicts.
   */
  void refactoringDone(@Nonnull String refactoringId, @Nullable RefactoringEventData afterData);

  /**
   * Is fired when conflicts are detected. If the next event comes from the same refactoring then conflicts were ignored.
   * @param conflictsData should contain string representation of the conflicts
   */
  void conflictsDetected(@Nonnull String refactoringId, @Nonnull RefactoringEventData conflictsData);

  /**
   * Is fired when undoable action created on refactoring execution is undone.
   */
  void undoRefactoring(@Nonnull String refactoringId);
}
