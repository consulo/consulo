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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.JobLauncher;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInspection.InspectionEngine;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.LocalDescriptorsUtil;
import consulo.ide.impl.idea.codeInspection.ex.ProblemDescriptorImpl;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixWrapper;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.util.ConcurrencyUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.Language;
import consulo.language.editor.DaemonBundle;
import consulo.language.editor.Pass;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.UpdateHighlightersUtil;
import consulo.language.editor.impl.internal.highlight.Divider;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.editor.impl.internal.highlight.TransferToEDTQueue;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.language.editor.impl.internal.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.intention.HintAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.QuickFixAction;
import consulo.language.editor.internal.intention.EmptyIntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.file.FileViewProvider;
import consulo.language.file.inject.DocumentWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import consulo.util.lang.function.Condition;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author max
 */
public class LocalInspectionsPass extends ProgressableTextEditorHighlightingPass {
  private static final Logger LOG = Logger.getInstance(LocalInspectionsPass.class);
  public static final TextRange EMPTY_PRIORITY_RANGE = TextRange.EMPTY_RANGE;
  private static final Condition<PsiFile> SHOULD_INSPECT_FILTER = file -> HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(file);
  private final TextRange myPriorityRange;
  private final boolean myIgnoreSuppressed;
  private final ConcurrentMap<PsiFile, List<InspectionResult>> result = new ConcurrentHashMap<>();
  private static final String PRESENTABLE_NAME = DaemonBundle.message("pass.inspection");
  private volatile List<HighlightInfo> myInfos = Collections.emptyList();
  private final String myShortcutText;
  private final SeverityRegistrarImpl mySeverityRegistrar;
  private final InspectionProfileWrapper myProfileWrapper;
  private boolean myFailFastOnAcquireReadAction;

  public LocalInspectionsPass(@Nonnull PsiFile file,
                              @Nullable Document document,
                              int startOffset,
                              int endOffset,
                              @Nonnull TextRange priorityRange,
                              boolean ignoreSuppressed,
                              @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    super(file.getProject(), document, PRESENTABLE_NAME, file, null, new TextRange(startOffset, endOffset), true, highlightInfoProcessor);
    assert file.isPhysical() : "can't inspect non-physical file: " + file + "; " + file.getVirtualFile();
    myPriorityRange = priorityRange;
    myIgnoreSuppressed = ignoreSuppressed;
    setId(Pass.LOCAL_INSPECTIONS);

    final KeymapManager keymapManager = KeymapManager.getInstance();

    final Keymap keymap = keymapManager.getActiveKeymap();
    myShortcutText = keymap == null ? "" : "(" + KeymapUtil.getShortcutsText(keymap.getShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)) + ")";

    InspectionProfile profile = consulo.language.editor.inspection.scheme.InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();

    Function<InspectionProfile, InspectionProfileWrapper> custom = file.getUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY);

    myProfileWrapper = custom == null ? new InspectionProfileWrapper(profile) : custom.apply(profile);
    assert myProfileWrapper != null;
    mySeverityRegistrar = (SeverityRegistrarImpl)((SeverityProvider)myProfileWrapper.getInspectionProfile().getProfileManager()).getSeverityRegistrar();

    // initial guess
    setProgressLimit(300 * 2);
  }

  @Nonnull
  private PsiFile getFile() {
    //noinspection ConstantConditions
    return myFile;
  }

  @Override
  protected void collectInformationWithProgress(@Nonnull ProgressIndicator progress) {
    try {
      if (!HighlightingLevelManager.getInstance(myProject).shouldInspect(getFile())) return;
      inspect(getInspectionTools(myProfileWrapper), InspectionManager.getInstance(myProject), true, true, progress);
    }
    finally {
      disposeDescriptors();
    }
  }

  private void disposeDescriptors() {
    result.clear();
  }

  public void doInspectInBatch(@Nonnull final GlobalInspectionContextImpl context, @Nonnull final InspectionManager iManager, @Nonnull final List<LocalInspectionToolWrapper> toolWrappers) {
    final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
    inspect(new ArrayList<>(toolWrappers), iManager, false, false, progress);
    addDescriptorsFromInjectedResults(iManager, context);
    List<InspectionResult> resultList = result.get(getFile());
    if (resultList == null) return;
    for (InspectionResult inspectionResult : resultList) {
      LocalInspectionToolWrapper toolWrapper = inspectionResult.tool;
      for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
        addDescriptors(toolWrapper, descriptor, context);
      }
    }
  }

  private void addDescriptors(@Nonnull LocalInspectionToolWrapper toolWrapper, @Nonnull ProblemDescriptor descriptor, @Nonnull GlobalInspectionContextImpl context) {
    InspectionToolPresentation toolPresentation = context.getPresentation(toolWrapper);
    LocalDescriptorsUtil.addProblemDescriptors(Collections.singletonList(descriptor), toolPresentation, myIgnoreSuppressed, context, toolWrapper.getTool());
  }

  private void addDescriptorsFromInjectedResults(@Nonnull InspectionManager iManager, @Nonnull GlobalInspectionContextImpl context) {
    InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);

    for (Map.Entry<PsiFile, List<InspectionResult>> entry : result.entrySet()) {
      PsiFile file = entry.getKey();
      if (file == getFile()) continue; // not injected
      DocumentWindow documentRange = (DocumentWindow)documentManager.getDocument(file);
      List<InspectionResult> resultList = entry.getValue();
      for (InspectionResult inspectionResult : resultList) {
        LocalInspectionToolWrapper toolWrapper = inspectionResult.tool;
        for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {

          PsiElement psiElement = descriptor.getPsiElement();
          if (psiElement == null) continue;
          if (SuppressionUtil.inspectionResultSuppressed(psiElement, toolWrapper.getTool())) continue;
          List<TextRange> editables = ilManager.intersectWithAllEditableFragments(file, ((ProblemDescriptorBase)descriptor).getTextRange());
          for (TextRange editable : editables) {
            TextRange hostRange = documentRange.injectedToHost(editable);
            QuickFix[] fixes = descriptor.getFixes();
            LocalQuickFix[] localFixes = null;
            if (fixes != null) {
              localFixes = new LocalQuickFix[fixes.length];
              for (int k = 0; k < fixes.length; k++) {
                QuickFix fix = fixes[k];
                localFixes[k] = (LocalQuickFix)fix;
              }
            }
            ProblemDescriptor patchedDescriptor = iManager.createProblemDescriptor(getFile(), hostRange, descriptor.getDescriptionTemplate(), descriptor.getHighlightType(), true, localFixes);
            addDescriptors(toolWrapper, patchedDescriptor, context);
          }
        }
      }
    }
  }

  private void inspect(@Nonnull final List<LocalInspectionToolWrapper> toolWrappers,
                       @Nonnull final InspectionManager iManager,
                       final boolean isOnTheFly,
                       boolean failFastOnAcquireReadAction,
                       @Nonnull final ProgressIndicator progress) {
    myFailFastOnAcquireReadAction = failFastOnAcquireReadAction;
    if (toolWrappers.isEmpty()) return;

    List<Divider.DividedElements> allDivided = new ArrayList<>();
    Divider.divideInsideAndOutsideAllRoots(myFile, myRestrictRange, myPriorityRange, SHOULD_INSPECT_FILTER, new CommonProcessors.CollectProcessor<>(allDivided));
    List<PsiElement> inside = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> d.inside));
    List<PsiElement> outside = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> ContainerUtil.concat(d.outside, d.parents)));

    Set<String> elementDialectIds = InspectionEngine.calcElementDialectIds(inside, outside);
    Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedLanguageIds = InspectionEngine.getToolsToSpecifiedLanguages(toolWrappers);

    setProgressLimit(toolToSpecifiedLanguageIds.size() * 2L);
    final LocalInspectionToolSession session = new LocalInspectionToolSession(getFile(), myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset());

    List<InspectionContext> init = visitPriorityElementsAndInit(toolToSpecifiedLanguageIds, iManager, isOnTheFly, progress, inside, session, toolWrappers, elementDialectIds);
    inspectInjectedPsi(inside, isOnTheFly, progress, iManager, true, toolWrappers);
    visitRestElementsAndCleanup(progress, outside, session, init, elementDialectIds);
    inspectInjectedPsi(outside, isOnTheFly, progress, iManager, false, toolWrappers);

    progress.checkCanceled();

    myInfos = new ArrayList<>();
    addHighlightsFromResults(myInfos, progress);
  }

  @Nonnull
  private List<InspectionContext> visitPriorityElementsAndInit(@Nonnull Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedLanguageIds,
                                                               @Nonnull final InspectionManager iManager,
                                                               final boolean isOnTheFly,
                                                               @Nonnull final ProgressIndicator indicator,
                                                               @Nonnull final List<PsiElement> elements,
                                                               @Nonnull final LocalInspectionToolSession session,
                                                               @Nonnull List<LocalInspectionToolWrapper> wrappers,
                                                               @Nonnull final Set<String> elementDialectIds) {
    final List<InspectionContext> init = new ArrayList<>();
    List<Map.Entry<LocalInspectionToolWrapper, Set<String>>> entries = new ArrayList<>(toolToSpecifiedLanguageIds.entrySet());

    Processor<Map.Entry<LocalInspectionToolWrapper, Set<String>>> processor = pair -> {
      LocalInspectionToolWrapper toolWrapper = pair.getKey();
      Set<String> dialectIdsSpecifiedForTool = pair.getValue();
      ((ApplicationEx)ApplicationManager.getApplication())
              .executeByImpatientReader(() -> runToolOnElements(toolWrapper, dialectIdsSpecifiedForTool, iManager, isOnTheFly, indicator, elements, session, init, elementDialectIds));
      return true;
    };
    boolean result = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(entries, indicator, myFailFastOnAcquireReadAction, processor);
    if (!result) throw new ProcessCanceledException();
    return init;
  }

  private void runToolOnElements(@Nonnull final LocalInspectionToolWrapper toolWrapper,
                                 Set<String> dialectIdsSpecifiedForTool,
                                 @Nonnull final InspectionManager iManager,
                                 final boolean isOnTheFly,
                                 @Nonnull final ProgressIndicator indicator,
                                 @Nonnull final List<PsiElement> elements,
                                 @Nonnull final LocalInspectionToolSession session,
                                 @Nonnull List<InspectionContext> init,
                                 @Nonnull Set<String> elementDialectIds) {
    indicator.checkCanceled();

    ApplicationManager.getApplication().assertReadAccessAllowed();
    final LocalInspectionTool tool = toolWrapper.getTool();
    final boolean[] applyIncrementally = {isOnTheFly};
    ProblemsHolder holder = new ProblemsHolder(iManager, getFile(), isOnTheFly) {
      @Override
      public void registerProblem(@Nonnull ProblemDescriptor descriptor) {
        super.registerProblem(descriptor);
        if (applyIncrementally[0]) {
          addDescriptorIncrementally(descriptor, toolWrapper, indicator);
        }
      }
    };

    PsiElementVisitor visitor = InspectionEngine.createVisitorAndAcceptElements(tool, holder, isOnTheFly, session, elements, elementDialectIds, dialectIdsSpecifiedForTool);

    synchronized (init) {
      init.add(new InspectionContext(toolWrapper, holder, holder.getResultCount(), visitor, dialectIdsSpecifiedForTool));
    }
    advanceProgress(1);

    if (holder.hasResults()) {
      appendDescriptors(getFile(), holder.getResults(), toolWrapper);
    }
    applyIncrementally[0] = false; // do not apply incrementally outside visible range
  }

  private void visitRestElementsAndCleanup(@Nonnull final ProgressIndicator indicator,
                                           @Nonnull final List<PsiElement> elements,
                                           @Nonnull final LocalInspectionToolSession session,
                                           @Nonnull List<InspectionContext> init,
                                           @Nonnull final Set<String> elementDialectIds) {
    Processor<InspectionContext> processor = context -> {
      indicator.checkCanceled();
      ApplicationManager.getApplication().assertReadAccessAllowed();
      InspectionEngine.acceptElements(elements, context.visitor, elementDialectIds, context.dialectIdsSpecifiedForTool);
      advanceProgress(1);
      context.tool.getTool().inspectionFinished(session, context.holder);

      if (context.holder.hasResults()) {
        List<ProblemDescriptor> allProblems = context.holder.getResults();
        List<ProblemDescriptor> restProblems = allProblems.subList(context.problemsSize, allProblems.size());
        appendDescriptors(getFile(), restProblems, context.tool);
      }
      return true;
    };
    boolean result = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(init, indicator, myFailFastOnAcquireReadAction, processor);
    if (!result) {
      throw new ProcessCanceledException();
    }
  }

  void inspectInjectedPsi(@Nonnull final List<PsiElement> elements,
                          final boolean onTheFly,
                          @Nonnull final ProgressIndicator indicator,
                          @Nonnull final InspectionManager iManager,
                          final boolean inVisibleRange,
                          @Nonnull final List<LocalInspectionToolWrapper> wrappers) {
    final Set<PsiFile> injected = new HashSet<>();
    for (PsiElement element : elements) {
      InjectedLanguageUtil.enumerate(element, getFile(), false, (injectedPsi, places) -> injected.add(injectedPsi));
    }
    if (injected.isEmpty()) return;
    Processor<PsiFile> processor = injectedPsi -> {
      doInspectInjectedPsi(injectedPsi, onTheFly, indicator, iManager, inVisibleRange, wrappers);
      return true;
    };
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(new ArrayList<>(injected), indicator, myFailFastOnAcquireReadAction, processor)) {
      throw new ProcessCanceledException();
    }
  }

  @Nullable
  private HighlightInfoImpl highlightInfoFromDescriptor(@Nonnull ProblemDescriptor problemDescriptor,
                                                        @Nonnull HighlightInfoType highlightInfoType,
                                                        @Nonnull String message,
                                                        String toolTip,
                                                        PsiElement psiElement) {
    TextRange textRange = ((ProblemDescriptorBase)problemDescriptor).getTextRange();
    if (textRange == null || psiElement == null) return null;
    boolean isFileLevel = psiElement instanceof PsiFile && textRange.equals(psiElement.getTextRange());

    final HighlightSeverity severity = highlightInfoType.getSeverity(psiElement);
    TextAttributes attributes = mySeverityRegistrar.getTextAttributesBySeverity(severity);
    HighlightInfoImpl.Builder b = HighlightInfoImpl.newHighlightInfo(highlightInfoType).range(psiElement, textRange.getStartOffset(), textRange.getEndOffset()).description(message).severity(severity);
    if (toolTip != null) b.escapedToolTip(toolTip);
    if (attributes != null) b.textAttributes(attributes);
    if (problemDescriptor.isAfterEndOfLine()) b.endOfLine();
    if (isFileLevel) b.fileLevelAnnotation();
    if (problemDescriptor.getProblemGroup() != null) b.problemGroup(problemDescriptor.getProblemGroup());

    return (HighlightInfoImpl)b.create();
  }

  private final Map<TextRange, RangeMarker> ranges2markersCache = new HashMap<>();
  private final TransferToEDTQueue<Trinity<ProblemDescriptor, LocalInspectionToolWrapper, ProgressIndicator>> myTransferToEDTQueue =
          new TransferToEDTQueue<>("Apply inspection results", new Processor<Trinity<ProblemDescriptor, LocalInspectionToolWrapper, ProgressIndicator>>() {
            private final InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
            private final InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
            private final List<HighlightInfo> infos = new ArrayList<>(2);
            private final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);

            @Override
            public boolean process(Trinity<ProblemDescriptor, LocalInspectionToolWrapper, ProgressIndicator> trinity) {
              ProgressIndicator indicator = trinity.getThird();
              if (indicator.isCanceled()) {
                return false;
              }

              ProblemDescriptor descriptor = trinity.first;
              LocalInspectionToolWrapper tool = trinity.second;
              PsiElement psiElement = descriptor.getPsiElement();
              if (psiElement == null) return true;
              PsiFile file = psiElement.getContainingFile();
              Document thisDocument = documentManager.getDocument(file);

              HighlightSeverity severity = inspectionProfile.getErrorLevel(HighlightDisplayKey.find(tool.getShortName()), file).getSeverity();

              infos.clear();
              createHighlightsForDescriptor(infos, emptyActionRegistered, ilManager, file, thisDocument, tool, severity, descriptor, psiElement);
              for (HighlightInfo info : infos) {
                final EditorColorsScheme colorsScheme = getColorsScheme();
                UpdateHighlightersUtilImpl
                        .addHighlighterToEditorIncrementally(myProject, myDocument, getFile(), myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), (HighlightInfoImpl)info, colorsScheme,
                                                             getId(), ranges2markersCache);
              }

              return true;
            }
          }, myProject.getDisposed(), 200);

  private final Set<Pair<TextRange, String>> emptyActionRegistered = Collections.synchronizedSet(new HashSet<Pair<TextRange, String>>());

  private void addDescriptorIncrementally(@Nonnull final ProblemDescriptor descriptor, @Nonnull final LocalInspectionToolWrapper tool, @Nonnull final ProgressIndicator indicator) {
    if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(descriptor.getPsiElement(), tool.getTool())) {
      return;
    }
    myTransferToEDTQueue.offer(Trinity.create(descriptor, tool, indicator));
  }

  private void appendDescriptors(@Nonnull PsiFile file, @Nonnull List<ProblemDescriptor> descriptors, @Nonnull LocalInspectionToolWrapper tool) {
    for (ProblemDescriptor descriptor : descriptors) {
      if (descriptor == null) {
        LOG.error("null descriptor. all descriptors(" + descriptors.size() + "): " + descriptors + "; file: " + file + " (" + file.getVirtualFile() + "); tool: " + tool);
      }
    }
    InspectionResult result = new InspectionResult(tool, descriptors);
    appendResult(file, result);
  }

  private void appendResult(@Nonnull PsiFile file, @Nonnull InspectionResult result) {
    List<InspectionResult> resultList = this.result.get(file);
    if (resultList == null) {
      resultList = ConcurrencyUtil.cacheOrGet(this.result, file, new ArrayList<>());
    }
    synchronized (resultList) {
      resultList.add(result);
    }
  }

  @Override
  protected void applyInformationWithProgress() {
    UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), myInfos, getColorsScheme(), getId());
  }

  private void addHighlightsFromResults(@Nonnull List<HighlightInfo> outInfos, @Nonnull ProgressIndicator indicator) {
    InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
    InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
    Set<Pair<TextRange, String>> emptyActionRegistered = new HashSet<>();

    for (Map.Entry<PsiFile, List<InspectionResult>> entry : result.entrySet()) {
      indicator.checkCanceled();
      PsiFile file = entry.getKey();
      Document documentRange = documentManager.getDocument(file);
      if (documentRange == null) continue;
      List<InspectionResult> resultList = entry.getValue();
      synchronized (resultList) {
        for (InspectionResult inspectionResult : resultList) {
          indicator.checkCanceled();
          LocalInspectionToolWrapper tool = inspectionResult.tool;
          HighlightSeverity severity = inspectionProfile.getErrorLevel(HighlightDisplayKey.find(tool.getShortName()), file).getSeverity();
          for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
            indicator.checkCanceled();
            PsiElement element = descriptor.getPsiElement();
            if (element != null) {
              createHighlightsForDescriptor(outInfos, emptyActionRegistered, ilManager, file, documentRange, tool, severity, descriptor, element);
            }
          }
        }
      }
    }
  }

  private void createHighlightsForDescriptor(@Nonnull List<HighlightInfo> outInfos,
                                             @Nonnull Set<Pair<TextRange, String>> emptyActionRegistered,
                                             @Nonnull InjectedLanguageManager ilManager,
                                             @Nonnull PsiFile file,
                                             @Nonnull Document documentRange,
                                             @Nonnull LocalInspectionToolWrapper toolWrapper,
                                             @Nonnull HighlightSeverity severity,
                                             @Nonnull ProblemDescriptor descriptor,
                                             @Nonnull PsiElement element) {
    LocalInspectionTool tool = toolWrapper.getTool();
    if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(element, tool)) return;
    HighlightInfoType level = ProblemDescriptorUtil.highlightTypeFromDescriptor(descriptor, severity, mySeverityRegistrar);
    HighlightInfoImpl info = createHighlightInfo(descriptor, toolWrapper, level, emptyActionRegistered, element);
    if (info == null) return;

    PsiFile context = getTopLevelFileInBaseLanguage(element);
    PsiFile myContext = getTopLevelFileInBaseLanguage(getFile());
    if (context != getFile()) {
      LOG.error("Reported element " +
                element +
                " is not from the file '" +
                file +
                "' the inspection '" +
                toolWrapper +
                "' (" +
                tool.getClass() +
                ") " +
                "was invoked for. Message: '" +
                descriptor +
                "'.\n" +
                "Element' containing file: " +
                context +
                "\n" +
                "Inspection invoked for file: " +
                myContext +
                "\n");
    }
    boolean isInjected = file != getFile();
    if (!isInjected) {

      outInfos.add(info);
      return;
    }
    // todo we got to separate our "internal" prefixes/suffixes from user-defined ones
    // todo in the latter case the errors should be highlighted, otherwise not
    List<TextRange> editables = ilManager.intersectWithAllEditableFragments(file, new TextRange(info.startOffset, info.endOffset));
    for (TextRange editable : editables) {
      TextRange hostRange = ((DocumentWindow)documentRange).injectedToHost(editable);
      int start = hostRange.getStartOffset();
      int end = hostRange.getEndOffset();
      HighlightInfoImpl.Builder builder = HighlightInfoImpl.newHighlightInfo(info.type).range(element, start, end);
      String description = info.getDescription();
      if (description != null) {
        builder.description(description);
      }
      String toolTip = info.getToolTip();
      if (toolTip != null) {
        builder.escapedToolTip(toolTip);
      }
      HighlightInfoImpl patched = (HighlightInfoImpl)builder.createUnconditionally();
      if (patched.startOffset != patched.endOffset || info.startOffset == info.endOffset) {
        patched.setFromInjection(true);
        registerQuickFixes(toolWrapper, descriptor, patched, emptyActionRegistered);
        outInfos.add(patched);
      }
    }
  }

  private PsiFile getTopLevelFileInBaseLanguage(@Nonnull PsiElement element) {
    PsiFile file = InjectedLanguageManager.getInstance(myProject).getTopLevelFile(element);
    FileViewProvider viewProvider = file.getViewProvider();
    return viewProvider.getPsi(viewProvider.getBaseLanguage());
  }

  @Nullable
  private HighlightInfoImpl createHighlightInfo(@Nonnull ProblemDescriptor descriptor,
                                                @Nonnull LocalInspectionToolWrapper tool,
                                                @Nonnull HighlightInfoType level,
                                                @Nonnull Set<Pair<TextRange, String>> emptyActionRegistered,
                                                @Nonnull PsiElement element) {
    @NonNls String message = ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element);

    final HighlightDisplayKey key = HighlightDisplayKey.find(tool.getShortName());
    final InspectionProfile inspectionProfile = myProfileWrapper.getInspectionProfile();
    if (!inspectionProfile.isToolEnabled(key, getFile())) return null;

    HighlightInfoType type = new HighlightInfoType.HighlightInfoTypeImpl(level.getSeverity(element), level.getAttributesKey());
    final String plainMessage = message.startsWith("<html>") ? StringUtil.unescapeXml(XmlStringUtil.stripHtml(message).replaceAll("<[^>]*>", "")) : message;
    @NonNls final String link = " <a " +
                                "href=\"#inspection/" +
                                tool.getShortName() +
                                "\"" +
                                (UIUtil.isUnderDarcula() ? " color=\"7AB4C9\" " : "") +
                                ">" +
                                DaemonBundle.message("inspection.extended.description") +
                                "</a> " +
                                myShortcutText;

    @NonNls String tooltip = null;
    if (descriptor.showTooltip()) {
      tooltip = XmlStringUtil.wrapInHtml((message.startsWith("<html>") ? XmlStringUtil.stripHtml(message) : XmlStringUtil.escapeString(message)) + link);
    }
    HighlightInfoImpl highlightInfo = highlightInfoFromDescriptor(descriptor, type, plainMessage, tooltip, element);
    if (highlightInfo != null) {
      registerQuickFixes(tool, descriptor, highlightInfo, emptyActionRegistered);
    }
    return highlightInfo;
  }

  private static void registerQuickFixes(@Nonnull LocalInspectionToolWrapper tool,
                                         @Nonnull ProblemDescriptor descriptor,
                                         @Nonnull HighlightInfoImpl highlightInfo,
                                         @Nonnull Set<Pair<TextRange, String>> emptyActionRegistered) {
    final HighlightDisplayKey key = HighlightDisplayKey.find(tool.getShortName());
    boolean needEmptyAction = true;
    final QuickFix[] fixes = descriptor.getFixes();
    if (fixes != null && fixes.length > 0) {
      for (int k = 0; k < fixes.length; k++) {
        if (fixes[k] != null) { // prevent null fixes from var args
          QuickFixAction.registerQuickFixAction(highlightInfo, QuickFixWrapper.wrap(descriptor, k), key);
          needEmptyAction = false;
        }
      }
    }
    HintAction hintAction = descriptor instanceof ProblemDescriptorImpl ? ((ProblemDescriptorImpl)descriptor).getHintAction() : null;
    if (hintAction != null) {
      QuickFixAction.registerQuickFixAction(highlightInfo, hintAction, key);
      needEmptyAction = false;
    }
    if (((ProblemDescriptorBase)descriptor).getEnforcedTextAttributes() != null) {
      needEmptyAction = false;
    }
    if (needEmptyAction && emptyActionRegistered.add(Pair.create(highlightInfo.getFixTextRange(), tool.getShortName()))) {
      IntentionAction emptyIntentionAction = new EmptyIntentionAction(tool.getDisplayName());
      QuickFixAction.registerQuickFixAction(highlightInfo, emptyIntentionAction, key);
    }
  }

  @Nonnull
  private static List<PsiElement> getElementsFrom(@Nonnull PsiFile file) {
    final FileViewProvider viewProvider = file.getViewProvider();
    final Set<PsiElement> result = new LinkedHashSet<>();
    final PsiElementVisitor visitor = new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        ProgressManager.checkCanceled();
        PsiElement child = element.getFirstChild();
        if (child == null) {
          // leaf element
        }
        else {
          // composite element
          while (child != null) {
            child.accept(this);
            result.add(child);

            child = child.getNextSibling();
          }
        }
      }
    };
    for (Language language : viewProvider.getLanguages()) {
      final PsiFile psiRoot = viewProvider.getPsi(language);
      if (psiRoot == null || !HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(psiRoot)) {
        continue;
      }
      psiRoot.accept(visitor);
      result.add(psiRoot);
    }
    return new ArrayList<>(result);
  }


  @Nonnull
  List<LocalInspectionToolWrapper> getInspectionTools(@Nonnull InspectionProfileWrapper profile) {
    final InspectionToolWrapper[] toolWrappers = profile.getInspectionProfile().getInspectionTools(getFile());
    InspectionProfileWrapper.checkInspectionsDuplicates(toolWrappers);
    List<LocalInspectionToolWrapper> enabled = new ArrayList<>();
    for (InspectionToolWrapper toolWrapper : toolWrappers) {
      ProgressManager.checkCanceled();
      if (!profile.isToolEnabled(HighlightDisplayKey.find(toolWrapper.getShortName()), getFile())) continue;
      LocalInspectionToolWrapper wrapper = null;
      if (toolWrapper instanceof LocalInspectionToolWrapper) {
        wrapper = (LocalInspectionToolWrapper)toolWrapper;
      }
      else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
        final GlobalInspectionToolWrapper globalInspectionToolWrapper = (GlobalInspectionToolWrapper)toolWrapper;
        wrapper = globalInspectionToolWrapper.getSharedLocalInspectionToolWrapper();
      }
      if (wrapper == null) continue;

      if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(getFile(), wrapper.getTool())) {
        continue;
      }
      enabled.add(wrapper);
    }
    return enabled;
  }

  private void doInspectInjectedPsi(@Nonnull PsiFile injectedPsi,
                                    final boolean isOnTheFly,
                                    @Nonnull final ProgressIndicator indicator,
                                    @Nonnull InspectionManager iManager,
                                    final boolean inVisibleRange,
                                    @Nonnull List<LocalInspectionToolWrapper> wrappers) {
    final PsiElement host = InjectedLanguageManager.getInstance(injectedPsi.getProject()).getInjectionHost(injectedPsi);

    final List<PsiElement> elements = getElementsFrom(injectedPsi);
    if (elements.isEmpty()) {
      return;
    }
    Set<String> elementDialectIds = InspectionEngine.calcElementDialectIds(elements);
    Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedLanguageIds = InspectionEngine.getToolsToSpecifiedLanguages(wrappers);
    for (final Map.Entry<LocalInspectionToolWrapper, Set<String>> pair : toolToSpecifiedLanguageIds.entrySet()) {
      indicator.checkCanceled();
      final LocalInspectionToolWrapper wrapper = pair.getKey();
      final LocalInspectionTool tool = wrapper.getTool();
      if (host != null && myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(host, tool)) {
        continue;
      }
      ProblemsHolder holder = new ProblemsHolder(iManager, injectedPsi, isOnTheFly) {
        @Override
        public void registerProblem(@Nonnull ProblemDescriptor descriptor) {
          super.registerProblem(descriptor);
          if (isOnTheFly && inVisibleRange) {
            addDescriptorIncrementally(descriptor, wrapper, indicator);
          }
        }
      };

      LocalInspectionToolSession injSession = new LocalInspectionToolSession(injectedPsi, 0, injectedPsi.getTextLength());
      Set<String> dialectIdsSpecifiedForTool = pair.getValue();
      InspectionEngine.createVisitorAndAcceptElements(tool, holder, isOnTheFly, injSession, elements, elementDialectIds, dialectIdsSpecifiedForTool);
      tool.inspectionFinished(injSession, holder);
      List<ProblemDescriptor> problems = holder.getResults();
      if (!problems.isEmpty()) {
        appendDescriptors(injectedPsi, problems, wrapper);
      }
    }
  }

  @Override
  @Nonnull
  public List<HighlightInfo> getInfos() {
    return myInfos;
  }

  private static class InspectionResult {
    @Nonnull
    private final LocalInspectionToolWrapper tool;
    @Nonnull
    private final List<ProblemDescriptor> foundProblems;

    private InspectionResult(@Nonnull LocalInspectionToolWrapper tool, @Nonnull List<ProblemDescriptor> foundProblems) {
      this.tool = tool;
      this.foundProblems = new ArrayList<>(foundProblems);
    }
  }

  private static class InspectionContext {
    private InspectionContext(@Nonnull LocalInspectionToolWrapper tool, @Nonnull ProblemsHolder holder, int problemsSize,
                              // need this to diff between found problems in visible part and the rest
                              @Nonnull PsiElementVisitor visitor, @Nullable Set<String> dialectIdsSpecifiedForTool) {
      this.tool = tool;
      this.holder = holder;
      this.problemsSize = problemsSize;
      this.visitor = visitor;
      this.dialectIdsSpecifiedForTool = dialectIdsSpecifiedForTool;
    }

    @Nonnull
    private final LocalInspectionToolWrapper tool;
    @Nonnull
    private final ProblemsHolder holder;
    private final int problemsSize;
    @Nonnull
    private final PsiElementVisitor visitor;
    @Nullable
    private final Set<String> dialectIdsSpecifiedForTool;
  }
}
