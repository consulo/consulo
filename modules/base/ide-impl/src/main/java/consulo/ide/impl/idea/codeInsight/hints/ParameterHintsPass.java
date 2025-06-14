// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.impl.EditorScrollingPositionKeeper;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.editor.impl.internal.inlay.HintInfoFilter;
import consulo.language.editor.impl.internal.inlay.param.HintUtils;
import consulo.language.editor.impl.internal.inlay.param.MethodInfoExcludeListFilter;
import consulo.language.editor.inlay.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.CancellablePromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static consulo.ide.impl.idea.codeInsight.hints.ParameterHintsPassFactory.forceHintsUpdateOnNextPass;

// TODO This pass should be rewritten with new API
public final class ParameterHintsPass extends EditorBoundHighlightingPass {
    private final Int2ObjectMap<List<HintData>> myHints = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<String> myShowOnlyIfExistedBeforeHints = new Int2ObjectOpenHashMap<>();
    private final PsiElement myRootElement;
    private final HintInfoFilter myHintInfoFilter;
    private final boolean myForceImmediateUpdate;

    public ParameterHintsPass(@Nonnull PsiElement element,
                              @Nonnull Editor editor,
                              @Nonnull HintInfoFilter hintsFilter,
                              boolean forceImmediateUpdate) {
        super(editor, element.getContainingFile(), true);
        myRootElement = element;
        myHintInfoFilter = hintsFilter;
        myForceImmediateUpdate = forceImmediateUpdate;
    }

    /**
     * Updates inlays recursively for a given element.
     * Use {@link NonBlockingReadActionImpl#waitForAsyncTaskCompletion() } in tests to wait for the results.
     * <p>
     * Return promise in EDT.
     */
    public static @Nonnull CancellablePromise<?> asyncUpdate(@Nonnull PsiElement element, @Nonnull Editor editor) {
        MethodInfoExcludeListFilter filter = MethodInfoExcludeListFilter.forLanguage(element.getLanguage());
        AsyncPromise<Object> promise = new AsyncPromise<>();
        SmartPsiElementPointer<PsiElement> elementPtr = SmartPointerManager.getInstance(element.getProject())
            .createSmartPsiElementPointer(element);
        ReadAction.nonBlocking(() -> collectInlaysInPass(editor, filter, elementPtr))
            .finishOnUiThread(Application::getAnyModalityState, pass -> {
                if (pass != null) {
                    pass.applyInformationToEditor();
                }
                promise.setResult(null);
            })
            .submit(AppExecutorUtil.getAppExecutorService());
        return promise;
    }

    @RequiredReadAction
    private static ParameterHintsPass collectInlaysInPass(@Nonnull Editor editor,
                                                          MethodInfoExcludeListFilter filter,
                                                          SmartPsiElementPointer<PsiElement> elementPtr) {
        PsiElement element = elementPtr.getElement();
        if (element == null || editor.isDisposed()) {
            return null;
        }
        try {
            ParameterHintsPass pass = new ParameterHintsPass(element, editor, filter, true);
            pass.doCollectInformation(new ProgressIndicatorBase());
            return pass;
        }
        catch (IndexNotReadyException e) {
            return null;
        }
    }

    @Override
    @RequiredReadAction
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        myHints.clear();

        Language language = myFile.getLanguage();
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider == null || !provider.canShowHintsWhenDisabled() && !isEnabled(language) || DiffImplUtil.isDiffEditor(myEditor)) {
            return;
        }

        if (!HighlightingLevelManager.getInstance(myFile.getProject()).shouldHighlight(myFile)) {
            return;
        }

        provider.createTraversal(myRootElement).forEach(element -> process(element, provider));
    }

    private static boolean isEnabled(Language language) {
        return HintUtils.isParameterHintsEnabledForLanguage(language);
    }

    private void process(@Nonnull PsiElement element, @Nonnull InlayParameterHintsProvider provider) {
        List<InlayInfo> hints = provider.getParameterHints(element, myFile);
        if (hints.isEmpty()) {
            return;
        }
        HintInfo info = provider.getHintInfo(element, myFile);

        boolean showHints = info == null || info instanceof HintInfo.OptionInfo || myHintInfoFilter.showHint(info);

        Stream<InlayInfo> inlays = hints.stream();
        if (!showHints) {
            inlays = inlays.filter(inlayInfo -> !inlayInfo.isFilterByExcludeList());
        }

        inlays.forEach(hint -> {
            int offset = hint.getOffset();
            if (!canShowHintsAtOffset(offset)) {
                return;
            }
            if (ParameterNameHintsSuppressor.isSuppressedForImpl(myFile, hint)) {
                return;
            }

            String presentation = provider.getInlayPresentation(hint.getText());
            if (hint.isShowOnlyIfExistedBefore()) {
                myShowOnlyIfExistedBeforeHints.put(offset, presentation);
            }
            else {
                List<HintData> hintList = myHints.get(offset);
                if (hintList == null) {
                    myHints.put(offset, hintList = new ArrayList<>());
                }
                HintWidthAdjustment widthAdjustment = convertHintPresentation(hint.getWidthAdjustment(), provider);
                hintList.add(new HintData(presentation, hint.isRelatesToPrecedingText(), widthAdjustment));
            }
        });
    }

    private static HintWidthAdjustment convertHintPresentation(HintWidthAdjustment widthAdjustment,
                                                               InlayParameterHintsProvider provider) {
        if (widthAdjustment != null) {
            String hintText = widthAdjustment.getHintTextToMatch();
            if (hintText != null) {
                String adjusterHintPresentation = provider.getInlayPresentation(hintText);
                if (!hintText.equals(adjusterHintPresentation)) {
                    widthAdjustment = new HintWidthAdjustment(widthAdjustment.getEditorTextToMatch(),
                        adjusterHintPresentation,
                        widthAdjustment.getAdjustmentPosition());
                }
            }
        }
        return widthAdjustment;
    }

    @Override
    public void doApplyInformationToEditor() {
        EditorScrollingPositionKeeper.perform(myEditor, false, () -> {
            ParameterHintsPresentationManager manager = ParameterHintsPresentationManager.getInstance();
            List<Inlay<?>> hints = hintsInRootElementArea(manager);
            ParameterHintsUpdater updater = new ParameterHintsUpdater(myEditor, hints, myHints, myShowOnlyIfExistedBeforeHints, myForceImmediateUpdate);
            updater.update();
        });

        if (ParameterHintsUpdater.hintRemovalDelayed(myEditor)) {
            forceHintsUpdateOnNextPass(myEditor);
        }
        else if (myRootElement == myFile) {
            ParameterHintsPassFactory.putCurrentPsiModificationStamp(myEditor, myFile);
        }
    }

    @RequiredReadAction
    private @Nonnull List<Inlay<?>> hintsInRootElementArea(ParameterHintsPresentationManager manager) {
        TextRange range = myRootElement.getTextRange();
        int elementStart = range.getStartOffset();
        int elementEnd = range.getEndOffset();

        // Adding hints on the borders is allowed only in case root element is a document
        // See: canShowHintsAtOffset
        if (myDocument.getTextLength() != range.getLength()) {
            ++elementStart;
            --elementEnd;
        }

        return manager.getParameterHintsInRange(myEditor, elementStart, elementEnd);
    }

    /**
     * Adding hints on the borders of root element (at startOffset or endOffset)
     * is allowed only in the case when root element is a document
     *
     * @return true if a given offset can be used for hint rendering
     */
    @RequiredReadAction
    private boolean canShowHintsAtOffset(int offset) {
        TextRange rootRange = myRootElement.getTextRange();

        if (!rootRange.containsOffset(offset)) {
            return false;
        }
        if (offset > rootRange.getStartOffset() && offset < rootRange.getEndOffset()) {
            return true;
        }

        return myDocument.getTextLength() == rootRange.getLength();
    }

    static final class HintData {
        final String presentationText;
        final boolean relatesToPrecedingText;
        final HintWidthAdjustment widthAdjustment;

        HintData(@Nonnull String text, boolean relatesToPrecedingText, HintWidthAdjustment widthAdjustment) {
            presentationText = text;
            this.relatesToPrecedingText = relatesToPrecedingText;
            this.widthAdjustment = widthAdjustment;
        }

        @Override
        public @Nonnull String toString() {
            return '\'' + presentationText + '\'';
        }
    }
}