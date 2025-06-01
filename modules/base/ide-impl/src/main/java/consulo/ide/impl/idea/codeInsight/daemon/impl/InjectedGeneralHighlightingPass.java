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
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.JobLauncher;
import consulo.application.util.function.Processor;
import consulo.application.util.function.Processors;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributes;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.internal.highlight.Divider;
import consulo.language.editor.impl.internal.highlight.GeneralHighlightingPass;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoHolder;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.editor.util.CollectHighlightsUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageManagerImpl;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.inject.impl.internal.PlaceImpl;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class InjectedGeneralHighlightingPass extends GeneralHighlightingPass {
  private static final String PRESENTABLE_NAME = "Injected fragments";

  InjectedGeneralHighlightingPass(@Nonnull Project project,
                                  @Nonnull PsiFile file,
                                  @Nonnull Document document,
                                  int startOffset,
                                  int endOffset,
                                  boolean updateAll,
                                  @Nonnull ProperTextRange priorityRange,
                                  @Nullable Editor editor,
                                  @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    super(project, file, document, startOffset, endOffset, updateAll, priorityRange, editor, highlightInfoProcessor);
  }

  @Override
  public String getPresentableName() {
    return PRESENTABLE_NAME;
  }

  @Override
  protected void collectInformationWithProgress(@Nonnull final ProgressIndicator progress) {
    if (!Registry.is("editor.injected.highlighting.enabled")) return;

    List<Divider.DividedElements> allDivided = new ArrayList<>();
    Divider.divideInsideAndOutsideAllRoots(myFile, myRestrictRange, myPriorityRange, SHOULD_HIGHLIGHT_FILTER, allDivided::add);

    List<PsiElement> allInsideElements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> d.inside));
    List<PsiElement> allOutsideElements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> d.outside));

    // all infos for the "injected fragment for the host which is inside" are indeed inside
    // but some of the infos for the "injected fragment for the host which is outside" can be still inside
    Set<PsiFile> injected = getInjectedPsiFiles(allInsideElements, allOutsideElements, progress);
    setProgressLimit(injected.size());

    Set<HighlightInfo> injectedResult = new HashSet<>();
    if (!addInjectedPsiHighlights(injected, progress, Collections.synchronizedSet(injectedResult))) {
      throw new ProcessCanceledException();
    }

    Set<HighlightInfo> result;
    synchronized (injectedResult) {
      // sync here because all writes happened in another thread
      result = injectedResult;
    }
    final Set<HighlightInfo> gotHighlights = new HashSet<>(100);
    final List<HighlightInfo> injectionsOutside = new ArrayList<>(gotHighlights.size());
    for (HighlightInfo info : result) {
      if (myRestrictRange.contains(info)) {
        gotHighlights.add(info);
      }
      else {
        // nonconditionally apply injected results regardless whether they are in myStartOffset,myEndOffset
        injectionsOutside.add(info);
      }
    }

    if (!injectionsOutside.isEmpty()) {
      final ProperTextRange priorityIntersection = myPriorityRange.intersection(myRestrictRange);
      if ((!allInsideElements.isEmpty() || !gotHighlights.isEmpty()) && priorityIntersection != null) { // do not apply when there were no elements to highlight
        // clear infos found in visible area to avoid applying them twice
        final List<HighlightInfo> toApplyInside = new ArrayList<>(gotHighlights);
        myHighlights.addAll(toApplyInside);
        gotHighlights.clear();

        myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), toApplyInside, myPriorityRange, myRestrictRange, getId());
      }

      List<HighlightInfo> toApply = new ArrayList<>();
      for (HighlightInfo info : gotHighlights) {
        if (!myRestrictRange.contains(info)) continue;
        if (!myPriorityRange.contains(info)) {
          toApply.add(info);
        }
      }
      toApply.addAll(injectionsOutside);

      myHighlightInfoProcessor.highlightsOutsideVisiblePartAreProduced(myHighlightingSession, getEditor(), toApply, myRestrictRange, new ProperTextRange(0, myDocument.getTextLength()), getId());
    }
    else {
      // else apply only result (by default apply command) and only within inside
      myHighlights.addAll(gotHighlights);
      myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), myHighlights, myRestrictRange, myRestrictRange, getId());
    }
  }

  @Nonnull
  private Set<PsiFile> getInjectedPsiFiles(@Nonnull final List<? extends PsiElement> elements1, @Nonnull final List<? extends PsiElement> elements2, @Nonnull final ProgressIndicator progress) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    List<DocumentWindow> injected = InjectedLanguageManager.getInstance(myProject).getCachedInjectedDocumentsInRange(myFile, myFile.getTextRange());
    final Collection<PsiElement> hosts = new HashSet<>(elements1.size() + elements2.size() + injected.size());

    //rehighlight all injected PSI regardless the range,
    //since change in one place can lead to invalidation of injected PSI in (completely) other place.
    for (DocumentWindow documentRange : injected) {
      ProgressManager.checkCanceled();
      if (!documentRange.isValid()) continue;
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(documentRange);
      if (file == null) continue;
      PsiElement context = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);
      if (context != null && context.isValid() && !file.getProject().isDisposed() && (myUpdateAll || myRestrictRange.intersects(context.getTextRange()))) {
        hosts.add(context);
      }
    }
    InjectedLanguageManagerImpl injectedLanguageManager = InjectedLanguageManagerImpl.getInstanceImpl(myProject);
    Processor<PsiElement> collectInjectableProcessor = Processors.cancelableCollectProcessor(hosts);
    injectedLanguageManager.processInjectableElements(elements1, collectInjectableProcessor);
    injectedLanguageManager.processInjectableElements(elements2, collectInjectableProcessor);

    final Set<PsiFile> outInjected = new HashSet<>();
    final PsiLanguageInjectionHost.InjectedPsiVisitor visitor = (injectedPsi, places) -> {
      synchronized (outInjected) {
        outInjected.add(injectedPsi);
      }
    };
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(new ArrayList<>(hosts), progress, element -> {
      ApplicationManager.getApplication().assertReadAccessAllowed();
      ProgressManager.checkCanceled();
      InjectedLanguageManager.getInstance(myFile.getProject()).enumerateEx(element, myFile, false, visitor);
      return true;
    })) {
      throw new ProcessCanceledException();
    }
    synchronized (outInjected) {
      return outInjected;
    }
  }

  // returns false if canceled
  private boolean addInjectedPsiHighlights(@Nonnull final Set<? extends PsiFile> injectedFiles, @Nonnull final ProgressIndicator progress, @Nonnull final Collection<? super HighlightInfo> outInfos) {
    if (injectedFiles.isEmpty()) return true;
    final InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(myProject);
    final TextAttributes injectedAttributes = myGlobalScheme.getAttributes(EditorColors.INJECTED_LANGUAGE_FRAGMENT);

    return JobLauncher.getInstance()
            .invokeConcurrentlyUnderProgress(new ArrayList<>(injectedFiles), progress, injectedPsi -> addInjectedPsiHighlights(injectedPsi, injectedAttributes, outInfos, injectedLanguageManager));
  }

  private boolean addInjectedPsiHighlights(@Nonnull PsiFile injectedPsi,
                                           TextAttributes injectedAttributes,
                                           @Nonnull Collection<? super HighlightInfo> outInfos,
                                           @Nonnull InjectedLanguageManager injectedLanguageManager) {
    DocumentWindow documentWindow = (DocumentWindow)PsiDocumentManager.getInstance(myProject).getCachedDocument(injectedPsi);
    if (documentWindow == null) return true;
    PlaceImpl places = InjectedLanguageUtil.getShreds(injectedPsi);
    boolean addTooltips = places.size() < 100;
    for (PsiLanguageInjectionHost.Shred place : places) {
      PsiLanguageInjectionHost host = place.getHost();
      if (host == null) continue;
      TextRange textRange = place.getRangeInsideHost().shiftRight(host.getTextRange().getStartOffset());
      if (textRange.isEmpty()) continue;
      HighlightInfoImpl.Builder builder = HighlightInfoImpl.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_BACKGROUND).range(textRange);
      if (injectedAttributes != null && InjectedLanguageUtil.isHighlightInjectionBackground(host)) {
        builder.textAttributes(injectedAttributes);
      }
      if (addTooltips) {
        String desc = injectedPsi.getLanguage().getDisplayName() + ": " + injectedPsi.getText();
        builder.unescapedToolTip(desc);
      }
      HighlightInfoImpl info = (HighlightInfoImpl)builder.createUnconditionally();
      info.setFromInjection(true);
      outInfos.add(info);
    }

    HighlightInfoHolder holder = createInfoHolder(injectedPsi);
    runHighlightVisitorsForInjected(injectedPsi, holder);
    for (int i = 0; i < holder.size(); i++) {
      HighlightInfoImpl info = (HighlightInfoImpl)holder.get(i);
      final int startOffset = documentWindow.injectedToHost(info.startOffset);
      final TextRange fixedTextRange = getFixedTextRange(documentWindow, startOffset);
      addPatchedInfos(info, injectedPsi, documentWindow, injectedLanguageManager, fixedTextRange, outInfos);
    }
    int injectedStart = holder.size();
    highlightInjectedSyntax(injectedPsi, holder);
    for (int i = injectedStart; i < holder.size(); i++) {
      HighlightInfoImpl info = (HighlightInfoImpl)holder.get(i);
      final int startOffset = info.startOffset;
      final TextRange fixedTextRange = getFixedTextRange(documentWindow, startOffset);
      if (fixedTextRange == null) {
        info.setFromInjection(true);
        outInfos.add(info);
      }
      else {
        HighlightInfoImpl patched =
                new HighlightInfoImpl(info.forcedTextAttributes, info.forcedTextAttributesKey, info.type, fixedTextRange.getStartOffset(), fixedTextRange.getEndOffset(), info.getDescription(), info.getToolTip(), info.type.getSeverity(null),
                                      info.isAfterEndOfLine(), null, false, 0, info.getProblemGroup(), info.getInspectionToolId(), info.getGutterIconRenderer(), info.getGroup());
        patched.setFromInjection(true);
        outInfos.add(patched);
      }
    }

    if (!isDumbMode()) {
      List<HighlightInfoImpl> todos = new ArrayList<>();
      highlightTodos(injectedPsi, injectedPsi.getText(), 0, injectedPsi.getTextLength(), myPriorityRange, todos, todos);
      for (HighlightInfoImpl info : todos) {
        addPatchedInfos(info, injectedPsi, documentWindow, injectedLanguageManager, null, outInfos);
      }
    }
    advanceProgress(1);
    return true;
  }

  @Override
  protected void queueInfoToUpdateIncrementally(@Nonnull HighlightInfo info) {
    // do not send info to highlight immediately - we need to convert its offsets first
    // see addPatchedInfos()
  }

  /**
   *
   * @param documentWindow
   * @param startOffset
   * @return null means invalid
   */
  @Nullable
  private static TextRange getFixedTextRange(@Nonnull DocumentWindow documentWindow, int startOffset) {
    final TextRange fixedTextRange;
    TextRange textRange = documentWindow.getHostRange(startOffset);
    if (textRange == null) {
      // todo[cdr] check this fix. prefix/suffix code annotation case
      textRange = findNearestTextRange(documentWindow, startOffset);
      if (textRange == null) return null;
      final boolean isBefore = startOffset < textRange.getStartOffset();
      fixedTextRange = new ProperTextRange(isBefore ? textRange.getStartOffset() - 1 : textRange.getEndOffset(), isBefore ? textRange.getStartOffset() : textRange.getEndOffset() + 1);
    }
    else {
      fixedTextRange = null;
    }
    return fixedTextRange;
  }

  private static void addPatchedInfos(@Nonnull HighlightInfoImpl info,
                                      @Nonnull PsiFile injectedPsi,
                                      @Nonnull DocumentWindow documentWindow,
                                      @Nonnull InjectedLanguageManager injectedLanguageManager,
                                      @Nullable TextRange fixedTextRange,
                                      @Nonnull Collection<? super HighlightInfoImpl> out) {
    ProperTextRange textRange = new ProperTextRange(info.startOffset, info.endOffset);
    List<TextRange> editables = injectedLanguageManager.intersectWithAllEditableFragments(injectedPsi, textRange);
    for (TextRange editable : editables) {
      TextRange hostRange = fixedTextRange == null ? documentWindow.injectedToHost(editable) : fixedTextRange;

      boolean isAfterEndOfLine = info.isAfterEndOfLine();
      if (isAfterEndOfLine) {
        // convert injected afterEndOfLine to either host' afterEndOfLine or not-afterEndOfLine highlight of the injected fragment boundary
        int hostEndOffset = hostRange.getEndOffset();
        int lineNumber = documentWindow.getDelegate().getLineNumber(hostEndOffset);
        int hostLineEndOffset = documentWindow.getDelegate().getLineEndOffset(lineNumber);
        if (hostEndOffset < hostLineEndOffset) {
          // convert to non-afterEndOfLine
          isAfterEndOfLine = false;
          hostRange = new ProperTextRange(hostRange.getStartOffset(), hostEndOffset + 1);
        }
      }

      HighlightInfoImpl patched =
              new HighlightInfoImpl(info.forcedTextAttributes, info.forcedTextAttributesKey, info.type, hostRange.getStartOffset(), hostRange.getEndOffset(), info.getDescription(), info.getToolTip(), info.type.getSeverity(null), isAfterEndOfLine, null,
                                    false, 0, info.getProblemGroup(), info.getInspectionToolId(), info.getGutterIconRenderer(), info.getGroup());
      patched.setHint(info.hasHint());

      if (info.quickFixActionRanges != null) {
        for (consulo.util.lang.Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange> pair : info.quickFixActionRanges) {
          TextRange quickfixTextRange = pair.getSecond();
          List<TextRange> editableQF = injectedLanguageManager.intersectWithAllEditableFragments(injectedPsi, quickfixTextRange);
          for (TextRange editableRange : editableQF) {
            HighlightInfoImpl.IntentionActionDescriptor descriptor = pair.getFirst();
            if (patched.quickFixActionRanges == null) patched.quickFixActionRanges = new ArrayList<>();
            TextRange hostEditableRange = documentWindow.injectedToHost(editableRange);
            patched.quickFixActionRanges.add(Pair.create(descriptor, hostEditableRange));
          }
        }
      }
      patched.setFromInjection(true);
      out.add(patched);
    }
  }

  /**
   * finds the first nearest text range
   * @param documentWindow
   * @param startOffset
   * @return null means invalid
   */
  @Nullable
  private static TextRange findNearestTextRange(final DocumentWindow documentWindow, final int startOffset) {
    TextRange textRange = null;
    for (Segment marker : documentWindow.getHostRanges()) {
      TextRange curRange = ProperTextRange.create(marker);
      if (curRange.getStartOffset() > startOffset && textRange != null) break;
      textRange = curRange;
    }
    return textRange;
  }

  private void runHighlightVisitorsForInjected(@Nonnull PsiFile injectedPsi, @Nonnull HighlightInfoHolder holder) {
    List<HighlightVisitor> filtered = getHighlightVisitors(injectedPsi);
    try {
      final List<PsiElement> elements = CollectHighlightsUtil.getElementsInRange(injectedPsi, 0, injectedPsi.getTextLength());
      for (final HighlightVisitor visitor : filtered) {
        visitor.analyze(injectedPsi, true, holder, () -> {
          for (PsiElement element : elements) {
            ProgressManager.checkCanceled();
            visitor.visit(element);
          }
        });
      }
    }
    finally {
      incVisitorUsageCount(-1);
    }
  }

  private void highlightInjectedSyntax(@Nonnull PsiFile injectedPsi, @Nonnull HighlightInfoHolder holder) {
    List<InjectedLanguageUtil.TokenInfo> tokens = InjectedLanguageUtil.getHighlightTokens(injectedPsi);
    if (tokens == null) return;

    final TextAttributes defaultAttrs = myGlobalScheme.getAttributes(HighlighterColors.TEXT);

    PlaceImpl shreds = InjectedLanguageUtil.getShreds(injectedPsi);
    int shredIndex = -1;
    int injectionHostTextRangeStart = -1;
    for (InjectedLanguageUtil.TokenInfo token : tokens) {
      ProgressManager.checkCanceled();
      TextRange range = token.rangeInsideInjectionHost;
      if (range.getLength() == 0) continue;
      if (shredIndex != token.shredIndex) {
        shredIndex = token.shredIndex;
        PsiLanguageInjectionHost.Shred shred = shreds.get(shredIndex);
        PsiLanguageInjectionHost host = shred.getHost();
        if (host == null) return;
        injectionHostTextRangeStart = host.getTextRange().getStartOffset();
      }
      TextRange annRange = range.shiftRight(injectionHostTextRangeStart);

      // force attribute colors to override host' ones
      TextAttributes attributes = token.attributes;
      TextAttributes forcedAttributes;
      if (attributes == null || attributes.isEmpty() || attributes.equals(defaultAttrs)) {
        forcedAttributes = TextAttributes.ERASE_MARKER;
      }
      else {
        HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT).range(annRange).textAttributes(TextAttributes.ERASE_MARKER).createUnconditionally();
        holder.add(info);

        forcedAttributes = new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), attributes.getEffectColor(), attributes.getEffectType(), attributes.getFontType());
      }

      HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT).range(annRange).textAttributes(forcedAttributes).createUnconditionally();
      holder.add(info);
    }
  }

  @Override
  protected void applyInformationWithProgress() {
  }
}
