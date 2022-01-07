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
package com.intellij.codeInspection;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.daemon.impl.Divider;
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefManagerImpl;
import com.intellij.codeInspection.reference.RefVisitor;
import com.intellij.concurrency.JobLauncher;
import com.intellij.lang.Language;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveVisitor;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SmartHashSet;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class InspectionEngine {
  private static final Logger LOG = Logger.getInstance(InspectionEngine.class);
  private static final Set<Class<? extends LocalInspectionTool>> RECURSIVE_VISITOR_TOOL_CLASSES = ContainerUtil.newConcurrentSet();

  /**
   *
   * @param tool
   * @param holder
   * @param isOnTheFly
   * @param session
   * @param elements
   * @param elementDialectIds
   * @param dialectIdsSpecifiedForTool null means all accepted
   * @return
   */
  @Nonnull
  public static PsiElementVisitor createVisitorAndAcceptElements(@Nonnull LocalInspectionTool tool,
                                                                 @Nonnull ProblemsHolder holder,
                                                                 boolean isOnTheFly,
                                                                 @Nonnull LocalInspectionToolSession session,
                                                                 @Nonnull List<PsiElement> elements,
                                                                 @Nonnull Set<String> elementDialectIds,
                                                                 @Nullable Set<String> dialectIdsSpecifiedForTool) {
    PsiElementVisitor visitor = tool.buildVisitor(holder, isOnTheFly, session);
    //noinspection ConstantConditions
    if(visitor == null) {
      LOG.error("Tool " + tool + " (" + tool.getClass()+ ") must not return null from the buildVisitor() method");
    }
    else if (visitor instanceof PsiRecursiveVisitor && RECURSIVE_VISITOR_TOOL_CLASSES.add(tool.getClass())) {
      LOG.error("The visitor returned from LocalInspectionTool.buildVisitor() must not be recursive: " + tool);
    }

    tool.inspectionStarted(session, isOnTheFly);
    acceptElements(elements, visitor, elementDialectIds, dialectIdsSpecifiedForTool);
    return visitor;
  }

  /**
   * @param elements
   * @param elementVisitor
   * @param elementDialectIds
   * @param dialectIdsSpecifiedForTool null means all accepted
   */
  public static void acceptElements(@Nonnull List<PsiElement> elements,
                                    @Nonnull PsiElementVisitor elementVisitor,
                                    @Nonnull Set<String> elementDialectIds,
                                    @Nullable Set<String> dialectIdsSpecifiedForTool) {
    if (dialectIdsSpecifiedForTool != null && !intersect(elementDialectIds, dialectIdsSpecifiedForTool)) return;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, elementsSize = elements.size(); i < elementsSize; i++) {
      PsiElement element = elements.get(i);
      element.accept(elementVisitor);
      ProgressManager.checkCanceled();
    }
  }

  private static boolean intersect(@Nonnull Set<String> ids1, @Nonnull Set<String> ids2) {
    if (ids1.size() > ids2.size()) return intersect(ids2, ids1);
    for (String id : ids1) {
      if (ids2.contains(id)) return true;
    }
    return false;
  }

  @Nonnull
  public static List<ProblemDescriptor> inspect(@Nonnull final List<LocalInspectionToolWrapper> toolWrappers,
                                                @Nonnull final PsiFile file,
                                                @Nonnull final InspectionManager iManager,
                                                final boolean isOnTheFly,
                                                boolean failFastOnAcquireReadAction,
                                                @Nonnull final ProgressIndicator indicator) {
    final Map<String, List<ProblemDescriptor>> problemDescriptors = inspectEx(toolWrappers, file, iManager, isOnTheFly, failFastOnAcquireReadAction, indicator);

    final List<ProblemDescriptor> result = new ArrayList<>();
    for (List<ProblemDescriptor> group : problemDescriptors.values()) {
      result.addAll(group);
    }
    return result;
  }

  // public for Upsource
  // returns map (toolName -> problem descriptors)
  @Nonnull
  public static Map<String, List<ProblemDescriptor>> inspectEx(@Nonnull final List<LocalInspectionToolWrapper> toolWrappers,
                                                               @Nonnull final PsiFile file,
                                                               @Nonnull final InspectionManager iManager,
                                                               final boolean isOnTheFly,
                                                               boolean failFastOnAcquireReadAction,
                                                               @Nonnull final ProgressIndicator indicator) {
    if (toolWrappers.isEmpty()) return Collections.emptyMap();


    TextRange range = file.getTextRange();
    List<Divider.DividedElements> allDivided = new ArrayList<>();
    Divider.divideInsideAndOutsideAllRoots(file, range, range, Conditions.alwaysTrue(), new CommonProcessors.CollectProcessor<>(allDivided));

    List<PsiElement> elements = ContainerUtil.concat(
            (List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> ContainerUtil.concat(d.inside, d.outside, d.parents)));

    return inspectElements(toolWrappers, file, iManager, isOnTheFly, failFastOnAcquireReadAction, indicator, elements,
                           calcElementDialectIds(elements));
  }

  // returns map tool.shortName -> list of descriptors found
  @Nonnull
  static Map<String, List<ProblemDescriptor>> inspectElements(@Nonnull List<LocalInspectionToolWrapper> toolWrappers,
                                                              @Nonnull final PsiFile file,
                                                              @Nonnull final InspectionManager iManager,
                                                              final boolean isOnTheFly,
                                                              boolean failFastOnAcquireReadAction,
                                                              @Nonnull ProgressIndicator indicator,
                                                              @Nonnull final List<PsiElement> elements,
                                                              @Nonnull final Set<String> elementDialectIds) {
    TextRange range = file.getTextRange();
    final LocalInspectionToolSession session = new LocalInspectionToolSession(file, range.getStartOffset(), range.getEndOffset());

    Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedDialectIds = getToolsToSpecifiedLanguages(toolWrappers);
    List<Entry<LocalInspectionToolWrapper, Set<String>>> entries = new ArrayList<>(toolToSpecifiedDialectIds.entrySet());
    final Map<String, List<ProblemDescriptor>> resultDescriptors = new ConcurrentHashMap<>();
    Processor<Entry<LocalInspectionToolWrapper, Set<String>>> processor = entry -> {
      ProblemsHolder holder = new ProblemsHolder(iManager, file, isOnTheFly);
      final LocalInspectionTool tool = entry.getKey().getTool();
      Set<String> dialectIdsSpecifiedForTool = entry.getValue();
      createVisitorAndAcceptElements(tool, holder, isOnTheFly, session, elements, elementDialectIds, dialectIdsSpecifiedForTool);

      tool.inspectionFinished(session, holder);

      if (holder.hasResults()) {
        resultDescriptors.put(tool.getShortName(), ContainerUtil.filter(holder.getResults(), descriptor -> {
          PsiElement element = descriptor.getPsiElement();
          return element == null || !SuppressionUtil.inspectionResultSuppressed(element, tool);
        }));
      }

      return true;
    };
    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(entries, indicator, failFastOnAcquireReadAction, processor);

    return resultDescriptors;
  }

  @Nonnull
  public static List<ProblemDescriptor> runInspectionOnFile(@Nonnull final PsiFile file,
                                                            @Nonnull InspectionToolWrapper toolWrapper,
                                                            @Nonnull final GlobalInspectionContext inspectionContext) {
    final InspectionManager inspectionManager = InspectionManager.getInstance(file.getProject());
    toolWrapper.initialize(inspectionContext);
    RefManagerImpl refManager = (RefManagerImpl)inspectionContext.getRefManager();
    refManager.inspectionReadActionStarted();
    try {
      if (toolWrapper instanceof LocalInspectionToolWrapper) {
        return inspect(Collections.singletonList((LocalInspectionToolWrapper)toolWrapper), file, inspectionManager, false, false, new EmptyProgressIndicator());
      }
      if (toolWrapper instanceof GlobalInspectionToolWrapper) {
        final GlobalInspectionTool globalTool = ((GlobalInspectionToolWrapper)toolWrapper).getTool();
        final List<ProblemDescriptor> descriptors = new ArrayList<>();
        if (globalTool instanceof GlobalSimpleInspectionTool) {
          GlobalSimpleInspectionTool simpleTool = (GlobalSimpleInspectionTool)globalTool;
          ProblemsHolder problemsHolder = new ProblemsHolder(inspectionManager, file, false);
          ProblemDescriptionsProcessor collectProcessor = new ProblemDescriptionsProcessor() {
            @javax.annotation.Nullable
            @Override
            public CommonProblemDescriptor[] getDescriptions(@Nonnull RefEntity refEntity) {
              return descriptors.toArray(new CommonProblemDescriptor[descriptors.size()]);
            }

            @Override
            public void ignoreElement(@Nonnull RefEntity refEntity) {
              throw new RuntimeException();
            }

            @Override
            public void addProblemElement(@javax.annotation.Nullable RefEntity refEntity, @Nonnull CommonProblemDescriptor... commonProblemDescriptors) {
              if (!(refEntity instanceof RefElement)) return;
              PsiElement element = ((RefElement)refEntity).getPsiElement();
              convertToProblemDescriptors(element, commonProblemDescriptors, descriptors);
            }

            @Override
            public RefEntity getElement(@Nonnull CommonProblemDescriptor descriptor) {
              throw new RuntimeException();
            }
          };
          simpleTool.checkFile(file, inspectionManager, problemsHolder, inspectionContext, collectProcessor);
          return descriptors;
        }
        RefElement fileRef = refManager.getReference(file);
        final AnalysisScope scope = new AnalysisScope(file);
        assert fileRef != null;
        fileRef.accept(new RefVisitor(){
          @Override
          public void visitElement(@Nonnull RefEntity elem) {
            CommonProblemDescriptor[] elemDescriptors = globalTool.checkElement(elem, scope, inspectionManager, inspectionContext);
            if (elemDescriptors != null) {
              convertToProblemDescriptors(file, elemDescriptors, descriptors);
            }

            for (RefEntity child : elem.getChildren()) {
              child.accept(this);
            }
          }
        });
        return descriptors;
      }
    }
    finally {
      refManager.inspectionReadActionFinished();
      toolWrapper.cleanup(file.getProject());
      inspectionContext.cleanup();
    }
    return Collections.emptyList();
  }

  private static void convertToProblemDescriptors(@Nonnull PsiElement element,
                                                  @Nonnull CommonProblemDescriptor[] commonProblemDescriptors,
                                                  @Nonnull List<ProblemDescriptor> descriptors) {
    for (CommonProblemDescriptor common : commonProblemDescriptors) {
      if (common instanceof ProblemDescriptor) {
        descriptors.add((ProblemDescriptor)common);
      }
      else {
        ProblemDescriptorBase base =
                new ProblemDescriptorBase(element, element, common.getDescriptionTemplate(), (LocalQuickFix[])common.getFixes(),
                                          ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false, null, false, false);
        descriptors.add(base);
      }
    }
  }

  // returns map tool -> set of languages and dialects for that tool specified in plugin.xml
  @Nonnull
  public static Map<LocalInspectionToolWrapper, Set<String>> getToolsToSpecifiedLanguages(@Nonnull List<LocalInspectionToolWrapper> toolWrappers) {
    Map<LocalInspectionToolWrapper, Set<String>> toolToLanguages = new HashMap<>();
    for (LocalInspectionToolWrapper wrapper : toolWrappers) {
      ProgressManager.checkCanceled();
      Set<String> specifiedLangIds = getDialectIdsSpecifiedForTool(wrapper);
      toolToLanguages.put(wrapper, specifiedLangIds);
    }
    return toolToLanguages;
  }

  /**
   *
   * @param wrapper
   * @return null means not specified
   */
  @Nullable
  public static Set<String> getDialectIdsSpecifiedForTool(@Nonnull LocalInspectionToolWrapper wrapper) {
    String langId = wrapper.getLanguage();
    if (langId == null) {
      return null;
    }
    Language language = Language.findLanguageByID(langId);
    Set<String> result;
    if (language != null) {
      result = new SmartHashSet<String>();
      result.add(langId);
    }
    else {
      // unknown language in plugin.xml, ignore
      result = Collections.singleton(langId);
    }
    return result;
  }

  @Nonnull
  public static Set<String> calcElementDialectIds(@Nonnull List<PsiElement> inside, @Nonnull List<PsiElement> outside) {
    Set<String> dialectIds = new SmartHashSet<>();
    Set<Language> processedLanguages = new SmartHashSet<>();
    addDialects(inside, processedLanguages, dialectIds);
    addDialects(outside, processedLanguages, dialectIds);
    return dialectIds;
  }

  @Nonnull
  public static Set<String> calcElementDialectIds(@Nonnull List<PsiElement> elements) {
    Set<String> dialectIds = new SmartHashSet<>();
    Set<Language> processedLanguages = new SmartHashSet<>();
    addDialects(elements, processedLanguages, dialectIds);
    return dialectIds;
  }

  private static void addDialects(@Nonnull List<PsiElement> elements,
                                  @Nonnull Set<Language> processedLanguages,
                                  @Nonnull Set<String> dialectIds) {
    for (PsiElement element : elements) {
      Language language = element.getLanguage();
      dialectIds.add(language.getID());
    }
  }
}
