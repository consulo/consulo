/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefGraphAnnotator;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.editor.inspection.reference.RefVisitor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.JobDescriptor;
import consulo.language.editor.scope.AnalysisScope;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for global inspections. Global inspections work only in batch mode
 * (when the &quot;Analyze / Inspect Code&quot; is invoked) and can access the
 * complete graph of references between classes, methods and other elements in the scope
 * selected for the analysis.
 * <p>
 * Global inspections can use a shared local inspection tool for highlighting the cases
 * that do not need global analysis in the editor by implementing {@link #getSharedLocalInspectionTool()}
 * The shared local inspection tools shares settings and documentation with the global inspection tool.
 *
 * @author anna
 * @see LocalInspectionTool
 * @since 6.0
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class GlobalInspectionTool extends InspectionTool {
  @Nonnull
  @Override
  protected final String getSuppressId() {
    return super.getSuppressId();
  }

  /**
   * Returns the annotator which will receive callbacks while the reference graph
   * is being built. The annotator can be used to add additional markers to reference
   * graph nodes, through calls to {@link RefEntity#putUserData(Key, Object)}.
   *
   * @param refManager the reference graph manager instance
   * @return the annotator instance, or null if the inspection does not need any
   * additional markers or does not use the reference graph at all.
   * @see #isGraphNeeded
   */
  @Nullable
  @Deprecated
  public RefGraphAnnotator getAnnotator(@Nonnull RefManager refManager) {
    return null;
  }

  /**
   * Returns the annotator which will receive callbacks while the reference graph
   * is being built. The annotator can be used to add additional markers to reference
   * graph nodes, through calls to {@link RefEntity#putUserData(Key, Object)}.
   *
   * @param refManager the reference graph manager instance
   * @return the annotator instance, or null if the inspection does not need any
   * additional markers or does not use the reference graph at all.
   * @see #isGraphNeeded
   */
  @Nullable
  public RefGraphAnnotator getAnnotator(@Nonnull RefManager refManager, @Nonnull Object state) {
    return getAnnotator(refManager);
  }

  /**
   * Runs the global inspection. If building of the reference graph was requested by one of the
   * global inspection tools, this method is called after the graph has been built and before the
   * external usages are processed. The default implementation of the method passes each node
   * of the graph for processing to {@link #checkElement(RefEntity, AnalysisScope, InspectionManager, GlobalInspectionContext)}.
   *
   * @param scope                        the scope on which the inspection was run.
   * @param manager                      the inspection manager instance for the project on which the inspection was run.
   * @param globalContext                the context for the current global inspection run.
   * @param problemDescriptionsProcessor the collector for problems reported by the inspection
   */
  public void runInspection(@Nonnull final AnalysisScope scope,
                            @Nonnull final InspectionManager manager,
                            @Nonnull final GlobalInspectionContext globalContext,
                            @Nonnull final ProblemDescriptionsProcessor problemDescriptionsProcessor,
                            @Nonnull Object state) {
    globalContext.getRefManager().iterate(new RefVisitor() {
      @Override
      public void visitElement(@Nonnull RefEntity refEntity) {
        if (!globalContext.shouldCheck(refEntity, GlobalInspectionTool.this)) return;
        CommonProblemDescriptor[] descriptors = checkElement(refEntity, scope, manager, globalContext, problemDescriptionsProcessor, state);
        if (descriptors != null) {
          problemDescriptionsProcessor.addProblemElement(refEntity, descriptors);
        }
      }
    });
  }

  /**
   * Processes and reports problems for a single element of the completed reference graph.
   *
   * @param refEntity     the reference graph element to check for problems.
   * @param scope         the scope on which analysis was invoked.
   * @param manager       the inspection manager instance for the project on which the inspection was run.
   * @param globalContext the context for the current global inspection run.
   * @return the problems found for the element, or null if no problems were found.
   */
  @Nullable
  public CommonProblemDescriptor[] checkElement(@Nonnull RefEntity refEntity,
                                                @Nonnull AnalysisScope scope,
                                                @Nonnull InspectionManager manager,
                                                @Nonnull GlobalInspectionContext globalContext,
                                                @Nonnull Object state) {
    return null;
  }

  /**
   * Processes and reports problems for a single element of the completed reference graph.
   *
   * @param refEntity     the reference graph element to check for problems.
   * @param scope         the scope on which analysis was invoked.
   * @param manager       the inspection manager instance for the project on which the inspection was run.
   * @param globalContext the context for the current global inspection run.
   * @param processor     the collector for problems reported by the inspection
   * @return the problems found for the element, or null if no problems were found.
   */
  @Nullable
  public CommonProblemDescriptor[] checkElement(@Nonnull RefEntity refEntity,
                                                @Nonnull AnalysisScope scope,
                                                @Nonnull InspectionManager manager,
                                                @Nonnull GlobalInspectionContext globalContext,
                                                @Nonnull ProblemDescriptionsProcessor processor,
                                                @Nonnull Object state) {
    return checkElement(refEntity, scope, manager, globalContext, state);
  }

  /**
   * Checks if this inspection requires building of the reference graph. The reference graph
   * is built if at least one of the global inspection has requested that.
   *
   * @return true if the reference graph is required, false if the inspection does not use a
   * reference graph (refEntities) and uses some other APIs for its processing.
   */
  public boolean isGraphNeeded() {
    return true;
  }

  /**
   * True by default to ensure third party plugins are not broken
   *
   * @return true if inspection should be started ({@link #runInspection(AnalysisScope, InspectionManager, GlobalInspectionContext, ProblemDescriptionsProcessor)}) in ReadAction,
   * false if ReadAction is taken by inspection itself
   */
  public boolean isReadActionNeeded() {
    return true;
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  /**
   * Allows the inspection to process usages of analyzed classes outside the analysis scope.
   * This method is called after the reference graph has been built and after
   * the {@link #runInspection(AnalysisScope, InspectionManager, GlobalInspectionContext, ProblemDescriptionsProcessor)}
   * method has collected the list of problems for the current scope.
   * In order to save time when multiple inspections need to process
   * usages of the same classes and methods, usage searches are not performed directly, but
   * instead are queued for batch processing through
   * {@link GlobalJavaInspectionContext#enqueueClassUsagesProcessor} and similar methods. The method
   * can add new problems to <code>problemDescriptionsProcessor</code> or remove some of the problems
   * collected by {@link #runInspection(AnalysisScope, InspectionManager, GlobalInspectionContext, ProblemDescriptionsProcessor)}
   * by calling {@link ProblemDescriptionsProcessor#ignoreElement(RefEntity)}.
   *
   * @param manager                      the inspection manager instance for the project on which the inspection was run.
   * @param globalContext                the context for the current global inspection run.
   * @param problemDescriptionsProcessor the collector for problems reported by the inspection.
   * @param state                        the state of inspectionTool, see {@link #createStateProvider()}
   * @return true if a repeated call to this method is required after the queued usage processors
   * have completed work, false otherwise.
   */
  public boolean queryExternalUsagesRequests(@Nonnull InspectionManager manager,
                                             @Nonnull GlobalInspectionContext globalContext,
                                             @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
                                             @Nonnull Object state) {
    return false;
  }

  /**
   * Allows TeamCity plugin to reconstruct quickfixes from server side data
   *
   * @param hint a hint to distinguish different quick fixes for one problem
   * @return quickfix to be shown in editor when server side inspections are enabled
   */
  @Nullable
  public QuickFix getQuickFix(String hint) {
    return null;
  }

  /**
   * Allows TeamCity plugin to serialize quick fixes on server in order to reconstruct them in idea
   *
   * @param fix fix to be serialized
   * @return hint to be stored on server
   */
  @Nullable
  public String getHint(@Nonnull QuickFix fix) {
    return null;
  }

  /**
   * Allows additional description to refEntity problems
   *
   * @param buf       page content with problem description
   * @param refEntity entity to describe
   * @param composer  provides sample api to compose html
   */
  public void compose(@Nonnull StringBuffer buf, @Nonnull RefEntity refEntity, @Nonnull HTMLComposer composer) {
  }

  /**
   * @return JobDescriptors array to show inspection progress correctly. TotalAmount should be set (e.g. in
   * {@link #runInspection(AnalysisScope, InspectionManager, GlobalInspectionContext, ProblemDescriptionsProcessor)})
   * ProgressIndicator should progress with {@link GlobalInspectionContext#incrementJobDoneAmount(JobDescriptor, String)}
   */
  @Nullable
  public JobDescriptor[] getAdditionalJobs() {
    return null;
  }

  /**
   * In some cases we can do highlighting in annotator or high. visitor based on global inspection or use a shared local inspection tool
   */
  public boolean worksInBatchModeOnly() {
    return getSharedLocalInspectionTool() == null;
  }

  /**
   * Returns the local inspection tool used for highlighting in the editor. Meant for global inspections which have a local component.
   * The local inspection tool is not required to report on the exact same problems, and naturally can't use global analysis. The local
   * inspection tool is not used in batch mode.
   * <p>
   * For example a global inspection that reports a package could have a local inspection tool which highlights
   * the package statement in a file.
   */
  @Nullable
  public LocalInspectionTool getSharedLocalInspectionTool() {
    return null;
  }

  public void initialize(@Nonnull GlobalInspectionContext context, @Nonnull Object state) {
    initialize(context);
  }

  @Deprecated
  public void initialize(@Nonnull GlobalInspectionContext context) {
  }
}
