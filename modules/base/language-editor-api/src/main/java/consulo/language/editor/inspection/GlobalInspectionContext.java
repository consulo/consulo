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

package consulo.language.editor.inspection;

import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.editor.inspection.scheme.InspectionProfileEntry;
import consulo.language.editor.inspection.scheme.JobDescriptor;
import consulo.language.editor.inspection.scheme.StdJobDescriptors;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;

/**
 * The context for a global inspection run. Provides access to the reference graph
 * and allows to enqueue external usage processing.
 *
 * @author anna
 * @see GlobalInspectionTool#runInspection
 * @see GlobalInspectionTool#queryExternalUsagesRequests
 * @since 6.0
 */
public interface GlobalInspectionContext extends UserDataHolder {
  /**
   * Returns the reference graph for the global inspection run.
   *
   * @return the reference graph instance.
   */
  @Nonnull
  RefManager getRefManager();

  /**
   * Checks if the inspection with the specified ID is suppressed for the
   * specified reference graph node. Should not be called manually in normal case.
   * You need to check suppressions only when you use {@link GlobalInspectionTool#runInspection}
   *
   * @param entity           the reference graph node to check.
   * @param inspectionToolId the ID of the inspection to check.
   * @return true if the inspection is suppressed, false otherwise.
   * @deprecated use #shouldCheck instead
   */
  boolean isSuppressed(RefEntity entity, String inspectionToolId);

  /**
   * Checks if the inspection is suppressed for the specified reference graph node. Should not be called manually in normal case.
   * You need to check suppressions only when you use {@link GlobalInspectionTool#runInspection}
   *
   * @param entity the reference graph node to check.
   * @param tool   the inspection to check.
   * @return true if the inspection is suppressed, false otherwise.
   */
  boolean shouldCheck(RefEntity entity, GlobalInspectionTool tool);

  /**
   * Checks if the inspection with the specified ID is suppressed for the
   * specified PSI element.
   *
   * @param element          the PSI element to check.
   * @param inspectionToolId the ID of the inspection to check.
   * @return true if the inspection is suppressed, false otherwise.
   */
  boolean isSuppressed(@Nonnull PsiElement element, String inspectionToolId);

  @Nonnull
  Project getProject();

  <T> T getExtension(Key<T> key);

  void incrementJobDoneAmount(JobDescriptor job, String message);

  @Nonnull
  StdJobDescriptors getStdJobDescriptors();

  void cleanup();

  default boolean isToCheckMember(@Nonnull RefElement owner, @Nonnull InspectionProfileEntry tool) {
    return isToCheckFile(owner.getContainingFile(), tool) && !owner.isSuppressed(tool.getShortName());
  }

  boolean isToCheckFile(PsiFile file, @Nonnull InspectionProfileEntry tool);
}
