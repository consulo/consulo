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
package consulo.ide.impl.idea.codeInspection.ui;

import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixAction;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.HTMLComposerBase;
import consulo.language.editor.inspection.ProblemDescriptionsProcessor;
import consulo.language.editor.inspection.QuickFix;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefModule;
import consulo.language.editor.intention.IntentionAction;
import consulo.virtualFileSystem.status.FileStatus;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface InspectionToolPresentation extends ProblemDescriptionsProcessor {
  @Nonnull
  InspectionNode createToolNode(@Nonnull GlobalInspectionContextImpl globalInspectionContext,
                                @Nonnull InspectionNode node,
                                @Nonnull InspectionRVContentProvider provider,
                                @Nonnull InspectionTreeNode parentNode,
                                final boolean showStructure);

  void updateContent();

  boolean hasReportedProblems();

  Map<String, Set<RefEntity>> getContent();

  Map<String, Set<RefEntity>> getOldContent();

  void ignoreCurrentElement(RefEntity refEntity);

  void amnesty(RefEntity refEntity);

  void cleanup();

  void finalCleanup();

  boolean isGraphNeeded();

  boolean isElementIgnored(final RefEntity element);

  @Nonnull
  FileStatus getElementStatus(final RefEntity element);

  @Nonnull
  Collection<RefEntity> getIgnoredRefElements();

  @Nullable
  IntentionAction findQuickFixes(@Nonnull CommonProblemDescriptor descriptor, final String hint);

  @Nonnull
  HTMLComposerBase getComposer();

  void exportResults(@Nonnull final Element parentNode, @Nonnull RefEntity refEntity);

  Set<RefModule> getModuleProblems();

  @Nullable
  QuickFixAction[] getQuickFixes(@Nonnull final RefEntity[] refElements);

  @Nonnull
  Map<RefEntity, CommonProblemDescriptor[]> getProblemElements();

  @Nonnull
  Collection<CommonProblemDescriptor> getProblemDescriptors();

  @Nonnull
  FileStatus getProblemStatus(@Nonnull CommonProblemDescriptor descriptor);

  boolean isOldProblemsIncluded();

  @Nullable
  Map<RefEntity, CommonProblemDescriptor[]> getOldProblemElements();

  boolean isProblemResolved(RefEntity refEntity, CommonProblemDescriptor descriptor);

  void ignoreCurrentElementProblem(RefEntity refEntity, CommonProblemDescriptor descriptor);

  void addProblemElement(RefEntity refElement, boolean filterSuppressed, @Nonnull CommonProblemDescriptor... descriptions);

  void ignoreProblem(@Nonnull CommonProblemDescriptor descriptor, @Nonnull QuickFix fix);

  @Nonnull
  GlobalInspectionContextBase getContext();

  void ignoreProblem(RefEntity refEntity, CommonProblemDescriptor problem, int idx);

  @Nullable
  QuickFixAction[] extractActiveFixes(@Nonnull RefEntity[] refElements, @Nonnull Map<RefEntity, Set<QuickFix>> actions);

  void exportResults(@Nonnull final Element parentNode);
}
