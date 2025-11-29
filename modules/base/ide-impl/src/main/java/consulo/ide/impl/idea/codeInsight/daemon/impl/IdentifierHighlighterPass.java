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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.find.FindManager;
import consulo.find.FindUsagesHandler;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightHandlerBase;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightUsagesHandler;
import consulo.ide.impl.idea.find.findUsages.FindUsagesManager;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.TargetElementUtilExtender;
import consulo.language.editor.highlight.ReadWriteAccessDetector;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.*;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author yole
 */
public class IdentifierHighlighterPass extends TextEditorHighlightingPass {
    private static final Logger LOG = Logger.getInstance(IdentifierHighlighterPass.class);

    private final PsiFile myFile;
    private final Editor myEditor;
    private final Collection<TextRange> myReadAccessRanges = Collections.synchronizedList(new ArrayList<TextRange>());
    private final Collection<TextRange> myWriteAccessRanges = Collections.synchronizedList(new ArrayList<TextRange>());
    private final int myCaretOffset;

    IdentifierHighlighterPass(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull Editor editor) {
        super(project, editor.getDocument(), false);
        myFile = file;
        myEditor = editor;
        myCaretOffset = myEditor.getCaretModel().getOffset();
    }

    @Override
    @RequiredReadAction
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        HighlightUsagesHandlerBase<PsiElement> highlightUsagesHandler = HighlightUsagesHandler.createCustomHandler(myEditor, myFile);
        if (highlightUsagesHandler != null) {
            List<PsiElement> targets = highlightUsagesHandler.getTargets();
            highlightUsagesHandler.computeUsages(targets);
            List<TextRange> readUsages = highlightUsagesHandler.getReadUsages();
            for (TextRange readUsage : readUsages) {
                LOG.assertTrue(readUsage != null, "null text range from " + highlightUsagesHandler);
            }
            myReadAccessRanges.addAll(readUsages);
            List<TextRange> writeUsages = highlightUsagesHandler.getWriteUsages();
            for (TextRange writeUsage : writeUsages) {
                LOG.assertTrue(writeUsage != null, "null text range from " + highlightUsagesHandler);
            }
            myWriteAccessRanges.addAll(writeUsages);
            if (!highlightUsagesHandler.highlightReferences()) {
                return;
            }
        }

        Set<String> flags = new HashSet<>(Arrays.asList(
            TargetElementUtilExtender.ELEMENT_NAME_ACCEPTED,
            TargetElementUtilExtender.REFERENCED_ELEMENT_ACCEPTED
        ));
        PsiElement myTarget = TargetElementUtil.findTargetElement(myEditor, flags, myCaretOffset);

        if (myTarget == null) {
            if (!PsiDocumentManager.getInstance(myProject).isUncommited(myEditor.getDocument())) {
                // when document is committed, try to check injected stuff - it's fast
                Editor injectedEditor =
                    InjectedEditorManager.getInstance(myProject).getEditorForInjectedLanguageNoCommit(myEditor, myFile, myCaretOffset);
                myTarget = TargetElementUtil.findTargetElement(injectedEditor, flags, injectedEditor.getCaretModel().getOffset());
            }
        }

        if (myTarget != null) {
            highlightTargetUsages(myTarget);
        }
        else {
            PsiReference ref = TargetElementUtil.findReference(myEditor);
            if (ref instanceof PsiPolyVariantReference) {
                if (!ref.getElement().isValid()) {
                    throw new PsiInvalidElementAccessException(
                        ref.getElement(),
                        "Invalid element in " + ref + " of " + ref.getClass() + "; editor=" + myEditor
                    );
                }
                ResolveResult[] results = ((PsiPolyVariantReference) ref).multiResolve(false);
                if (results.length > 0) {
                    for (ResolveResult result : results) {
                        PsiElement target = result.getElement();
                        if (target != null) {
                            if (!target.isValid()) {
                                throw new PsiInvalidElementAccessException(
                                    target,
                                    "Invalid element returned from " + ref + " of " + ref.getClass() + "; editor=" + myEditor
                                );
                            }
                            highlightTargetUsages(target);
                        }
                    }
                }
            }

        }
    }

    /**
     * Returns read and write usages of psi element inside a single element
     *
     * @param target     target psi element
     * @param psiElement psi element to search in
     * @return a pair where first element is read usages and second is write usages
     */
    @Nonnull
    @RequiredReadAction
    public static Couple<Collection<TextRange>> getHighlightUsages(
        @Nonnull PsiElement target,
        PsiElement psiElement,
        boolean withDeclarations
    ) {
        return getUsages(target, psiElement, withDeclarations, true);
    }

    /**
     * Returns usages of psi element inside a single element
     *
     * @param target     target psi element
     * @param psiElement psi element to search in
     */
    @Nonnull
    @RequiredReadAction
    public static Collection<TextRange> getUsages(@Nonnull PsiElement target, PsiElement psiElement, boolean withDeclarations) {
        return getUsages(target, psiElement, withDeclarations, false).first;
    }

    @Nonnull
    @RequiredReadAction
    private static Couple<Collection<TextRange>> getUsages(
        @Nonnull PsiElement target,
        PsiElement psiElement,
        boolean withDeclarations,
        boolean detectAccess
    ) {
        List<TextRange> readRanges = new ArrayList<>();
        List<TextRange> writeRanges = new ArrayList<>();
        ReadWriteAccessDetector detector = detectAccess ? ReadWriteAccessDetector.findDetector(target) : null;
        FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(target.getProject())).getFindUsagesManager();
        FindUsagesHandler findUsagesHandler = findUsagesManager.getFindUsagesHandler(target, true);
        LocalSearchScope scope = new LocalSearchScope(psiElement);
        Collection<PsiReference> refs = findUsagesHandler != null
            ? findUsagesHandler.findReferencesToHighlight(target, scope)
            : ReferencesSearch.search(target, scope).findAll();
        for (PsiReference psiReference : refs) {
            if (psiReference == null) {
                LOG.error("Null reference returned, findUsagesHandler=" + findUsagesHandler + "; target=" + target + " of " + target.getClass());
                continue;
            }
            List<TextRange> destination;
            if (detector == null || detector.getReferenceAccess(target, psiReference) == ReadWriteAccessDetector.Access.Read) {
                destination = readRanges;
            }
            else {
                destination = writeRanges;
            }
            HighlightUsagesHandler.collectRangesToHighlight(psiReference, destination);
        }

        if (withDeclarations) {
            TextRange declRange = HighlightUsagesHandler.getNameIdentifierRange(psiElement.getContainingFile(), target);
            if (declRange != null) {
                if (detector != null && detector.isDeclarationWriteAccess(target)) {
                    writeRanges.add(declRange);
                }
                else {
                    readRanges.add(declRange);
                }
            }
        }

        return Couple.<Collection<TextRange>>of(readRanges, writeRanges);
    }

  @RequiredReadAction
  private void highlightTargetUsages(@Nonnull PsiElement target) {
        Couple<Collection<TextRange>> usages = getHighlightUsages(target, myFile, true);
        myReadAccessRanges.addAll(usages.first);
        myWriteAccessRanges.addAll(usages.second);
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        boolean virtSpace = TargetElementUtil.inVirtualSpace(myEditor, myEditor.getCaretModel().getOffset());
        List<HighlightInfo> infos = virtSpace ? Collections.<HighlightInfo>emptyList() : getHighlights();
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, 0, myFile.getTextLength(), infos, getColorsScheme(), getId());
    }

    private List<HighlightInfo> getHighlights() {
        if (myReadAccessRanges.isEmpty() && myWriteAccessRanges.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Pair<Object, TextRange>> existingMarkupTooltips = new HashSet<>();
        for (RangeHighlighter highlighter : myEditor.getMarkupModel().getAllHighlighters()) {
            existingMarkupTooltips.add(Pair.create(
                highlighter.getErrorStripeTooltip(),
                new TextRange(highlighter.getStartOffset(), highlighter.getEndOffset())
            ));
        }

        List<HighlightInfo> result = new ArrayList<>(myReadAccessRanges.size() + myWriteAccessRanges.size());
        for (TextRange range : myReadAccessRanges) {
            ContainerUtil.addIfNotNull(
                result,
                createHighlightInfo(range, HighlightInfoType.ELEMENT_UNDER_CARET_READ, existingMarkupTooltips)
            );
        }
        for (TextRange range : myWriteAccessRanges) {
            ContainerUtil.addIfNotNull(
                result,
                createHighlightInfo(range, HighlightInfoType.ELEMENT_UNDER_CARET_WRITE, existingMarkupTooltips)
            );
        }
        return result;
    }

    private HighlightInfo createHighlightInfo(
        TextRange range,
        HighlightInfoType type,
        Set<Pair<Object, TextRange>> existingMarkupTooltips
    ) {
        int start = range.getStartOffset();
        String tooltip =
            start <= myDocument.getTextLength() ? HighlightHandlerBase.getLineTextErrorStripeTooltip(myDocument, start, false) : null;
        String unescapedTooltip = existingMarkupTooltips.contains(new Pair<Object, TextRange>(tooltip, range)) ? null : tooltip;
        HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(type).range(range);
        if (unescapedTooltip != null) {
            builder.unescapedToolTip(unescapedTooltip);
        }
        return builder.createUnconditionally();
    }

    public static void clearMyHighlights(Document document, Project project) {
        MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);
        for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
            Object tooltip = highlighter.getErrorStripeTooltip();
            if (!(tooltip instanceof HighlightInfoImpl info)) {
                continue;
            }
            if (info.getType() == HighlightInfoType.ELEMENT_UNDER_CARET_READ || info.getType() == HighlightInfoType.ELEMENT_UNDER_CARET_WRITE) {
                highlighter.dispose();
            }
        }
    }
}
