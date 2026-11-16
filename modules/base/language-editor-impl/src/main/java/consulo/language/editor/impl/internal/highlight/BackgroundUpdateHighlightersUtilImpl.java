// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.internal.SweepProcessor;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.document.util.TextRangeScalarUtil;
import consulo.language.editor.impl.highlight.HighlightingSession;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.BackgroundUpdateHighlightersInternal;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.Comparing;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Document markup manipulation methods during the highlighting, in the background thread under read action.
 * Must be used inside the highlighting process only (e.g., in your
 * {@link consulo.language.editor.highlight.TextEditorHighlightingPass#collectInformation})
 */
@Singleton
@ServiceImpl
public final class BackgroundUpdateHighlightersUtilImpl implements BackgroundUpdateHighlightersInternal {
    private static final Logger LOG = Logger.getInstance(BackgroundUpdateHighlightersUtilImpl.class);

    @Override
    @RequiredReadAction
    public void setHighlightersToEditor(
        Project project,
        PsiFile psiFile,
        Document document,
        int startOffset,
        int endOffset,
        Collection<? extends HighlightInfo> highlights,
        int group
    ) {
        HighlightingSession session = HighlightingSessionImpl.getFromCurrentIndicator(psiFile);
        MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
        TextRange range = new TextRange(startOffset, endOffset);
        setHighlightersInRange(range, new ArrayList<>(highlights), markup, group, session);
    }

    private static void updateFileLevelHighlights(
        List<? extends HighlightInfo> fileLevelHighlights,
        int group,
        boolean cleanOldHighlights,
        PsiFile psiFile,
        HighlightingSession session
    ) {
        DaemonCodeAnalyzerInternal codeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(psiFile.getProject());
        boolean shouldUpdate = !fileLevelHighlights.isEmpty() || codeAnalyzer.hasFileLevelHighlights(group, psiFile);
        if (shouldUpdate) {
            ((HighlightingSessionImpl) session).updateFileLevelHighlights(fileLevelHighlights, group, cleanOldHighlights, psiFile);
        }
    }

    @RequiredReadAction
    public static void setHighlightersInRange(
        TextRange range,
        List<? extends HighlightInfo> infos,
        MarkupModelEx markup,
        int group,
        HighlightingSession session
    ) {
        Project project = session.getProject();
        Document document = session.getDocument();

        PsiFile psiFile = session.getPsiFile();
        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        boolean[] changed = {false};
        Map<TextRange, RangeMarker> range2markerCache = new HashMap<>(10);
        HighlighterRecycler.runWithRecycler(session, recycler -> {
            DaemonCodeAnalyzerInternal.processHighlights(document, project, null, range.getStartOffset(), range.getEndOffset(), i -> {
                HighlightInfoImpl info = (HighlightInfoImpl) i;
                if (info.getGroup() == group) {
                    int hiEnd = info.getEndOffset();
                    boolean willBeRemoved = range.containsRange(info.getStartOffset(), hiEnd)
                        || hiEnd == document.getTextLength() && range.getEndOffset() == hiEnd;
                    if (willBeRemoved) {
                        RangeHighlighterEx highlighter = info.getHighlighter();
                        if (highlighter != null) {
                            recycler.recycleHighlighter(info);
                        }
                    }
                }
                return true;
            });

            List<HighlightInfo> filteredInfos = UpdateHighlightersUtilImpl.HighlightInfoPostFilters.applyPostFilter(project, infos);
            Lists.quickSort(filteredInfos, UpdateHighlightersUtilImpl.BY_START_OFFSET_NODUPS);
            SweepProcessor.Generator<HighlightInfo> generator = processor -> ContainerUtil.process(filteredInfos, processor);
            List<HighlightInfo> fileLevelHighlights = new ArrayList<>();
            List<HighlightInfoImpl> infosToCreateHighlightersFor = new ArrayList<>(filteredInfos.size());
            SweepProcessor.sweep(generator, (offset, i, atStart, overlappingIntervals) -> {
                if (!atStart) {
                    return true;
                }
                HighlightInfoImpl info = (HighlightInfoImpl) i;
                if (info.isFileLevelAnnotation()) {
                    fileLevelHighlights.add(info);
                    changed[0] = true;
                    return true;
                }

                if (range.containsRange(info.getStartOffset(), info.getEndOffset())
                    && !UpdateHighlightersUtilImpl.isWarningCoveredByError(info, overlappingIntervals, severityRegistrar)) {
                    // have to create RangeHighlighter later, to avoid exposing them to the markup model immediately,
                    // thus messing the HighlightInfo.getStartOffset() leading to "sweep generator supplied infos in a wrong order" exception
                    infosToCreateHighlightersFor.add(info);
                    changed[0] = true;
                }
                return true;
            });
            for (HighlightInfoImpl info : infosToCreateHighlightersFor) {
                createOrReuseHighlighterFor(info, document, group, psiFile, markup, recycler, range2markerCache, severityRegistrar, session);
            }
            updateFileLevelHighlights(fileLevelHighlights, group, range.equalsToRange(0, document.getTextLength()), psiFile, session);
            changed[0] |= !recycler.isEmpty();
            if (changed[0]) {
                UpdateHighlightersUtilImpl.clearWhiteSpaceOptimizationFlag(document);
            }
        });
    }

    static long getRangeToCreateHighlighter(HighlightInfo info, Document document) {
        int infoStartOffset = info.getStartOffset();
        int infoEndOffset = info.getEndOffset();

        int docLength = document.getTextLength();
        if (infoEndOffset > docLength) {
            infoEndOffset = docLength;
            infoStartOffset = Math.min(infoStartOffset, infoEndOffset);
        }
        if (infoEndOffset == infoStartOffset && !info.isAfterEndOfLine()) {
            if (infoEndOffset == docLength) {
                return -1; // empty highlighter beyond file boundaries
            }
            infoEndOffset++; //show something in case of empty HighlightInfo
        }
        return TextRangeScalarUtil.toScalarRange(infoStartOffset, infoEndOffset);
    }

    /**
     * {@code synchronized} here is to avoid a race when {@code pass1} and {@code pass2} called naked {@link #setHighlightersInRange} simultaneously,
     * and one of them recycled the highlighter while the other decided to dispose it,
     * which could cause calling {@link #changeAttributes} on disposed highlighter and throwing NPE.
     */
    private static void createOrReuseHighlighterFor(
        HighlightInfoImpl info,
        Document document,
        int group,
        PsiFile psiFile,
        MarkupModelEx markup,
        HighlighterRecycler recycler,
        Map<TextRange, RangeMarker> range2markerCache,
        SeverityRegistrar severityRegistrar,
        HighlightingSession session
    ) {
        synchronized (info) {
            assert !info.isFileLevelAnnotation();
            long finalInfoRange = getRangeToCreateHighlighter(info, document);
            if (finalInfoRange == -1) {
                return;
            }
            info.setGroup(group);

            int layer = UpdateHighlightersUtilImpl.getLayer(info, severityRegistrar);
            int infoStartOffset = TextRangeScalarUtil.startOffset(finalInfoRange);
            int infoEndOffset = TextRangeScalarUtil.endOffset(finalInfoRange);
            TextRange finalInfoTextRange = TextRangeScalarUtil.create(finalInfoRange);

            EditorColorsScheme colorsScheme = session.getColorsScheme(); // if null, the global scheme will be used
            TextAttributes infoAttributes = info.getTextAttributes(psiFile, colorsScheme);
            Consumer<RangeHighlighterEx> changeAttributes = finalHighlighter -> {
                changeAttributes(finalHighlighter, info, colorsScheme, psiFile, infoAttributes);
                // the quick fixes ranges equal to the highlighter range share the highlighter as their marker
                range2markerCache.put(finalInfoTextRange, finalHighlighter);
                info.updateQuickFixFields(document, range2markerCache, finalInfoTextRange);
            };

            RangeHighlighterEx salvagedHighlighter =
                recycler.pickupHighlighterFromGarbageBin(infoStartOffset, infoEndOffset, layer, info.getDescription());

            RangeHighlighterEx highlighter;
            if (salvagedHighlighter == null) {
                highlighter = markup.addRangeHighlighterAndChangeAttributes(
                    null,
                    infoStartOffset,
                    infoEndOffset,
                    layer,
                    HighlighterTargetArea.EXACT_RANGE,
                    false,
                    changeAttributes
                );
            }
            else {
                highlighter = salvagedHighlighter;
                markup.changeAttributesInBatch(highlighter, changeAttributes);
            }

            range2markerCache.put(finalInfoTextRange, highlighter);

            if (LOG.isDebugEnabled()) {
                LOG.debug("createOrReuseHighlighter " + highlighter + (salvagedHighlighter == null ? "" : " (recycled)"));
            }
            if (infoAttributes != null) {
                TextAttributes actualAttributes = highlighter.getTextAttributes(colorsScheme);
                boolean attributesSet = Comparing.equal(infoAttributes, actualAttributes);
                if (!attributesSet) {
                    TextAttributes forcedTextAttributes = highlighter.getForcedTextAttributes();
                    highlighter.setTextAttributes(infoAttributes);
                    TextAttributes afterSet = highlighter.getTextAttributes(colorsScheme);
                    LOG.error("Expected to set " + infoAttributes + " but actual attributes are: " + actualAttributes +
                        "; forcedTextAttributes: '" + forcedTextAttributes + "'" +
                        "; colorsScheme: '" + (colorsScheme == null ? "[global]" : colorsScheme.getName()) + "'" +
                        "; highlighter:" + highlighter + " (" + highlighter.getClass() + ")" +
                        "; was reused from the bin: " + (salvagedHighlighter != null) +
                        "; markup: " + markup + " (" + markup.getClass() + ")" +
                        "; attributes after the second .setAttributes(): " + afterSet +
                        " (set " + (infoAttributes.equals(afterSet) ? "successfully" : "not successfully") + ")");
                }
            }
        }
    }

    public static void associateInfoAndHighlighter(HighlightInfoImpl info, RangeHighlighterEx highlighter) {
        if (info.getHighlighter() == null) {
            info.setHighlighter(highlighter);
        }
        assert info.getHighlighter() == highlighter;
        highlighter.setErrorStripeTooltip(info);
    }

    static void changeAttributes(
        RangeHighlighterEx highlighter,
        HighlightInfoImpl info,
        @Nullable EditorColorsScheme colorsScheme,
        PsiFile psiFile,
        @Nullable TextAttributes infoAttributes
    ) {
        if (!highlighter.isValid()) {
            return;
        }
        TextAttributesKey textAttributesKey =
            info.myForcedTextAttributesKey == null ? info.getType().getAttributesKey() : info.myForcedTextAttributesKey;
        highlighter.setTextAttributesKey(textAttributesKey);

        if (infoAttributes == TextAttributes.ERASE_MARKER ||
            infoAttributes != null && !infoAttributes.equals(highlighter.getTextAttributes(colorsScheme))) {
            highlighter.setTextAttributes(infoAttributes);
        }

        highlighter.setAfterEndOfLine(info.isAfterEndOfLine());

        ColorValue infoErrorStripeColor = info.getErrorStripeMarkColor(psiFile, colorsScheme);
        ColorValue attributesErrorStripeColor = infoAttributes != null ? infoAttributes.getErrorStripeColor() : null;
        if (infoErrorStripeColor != null && !infoErrorStripeColor.equals(attributesErrorStripeColor)) {
            highlighter.setErrorStripeMarkColor(infoErrorStripeColor);
        }

        associateInfoAndHighlighter(info, highlighter);
        GutterMark renderer = info.getGutterIconRenderer();
        highlighter.setGutterIconRenderer((GutterIconRenderer) renderer);

        if (HighlightInfoType.VISIBLE_IF_FOLDED.contains(info.getType())) {
            highlighter.setVisibleIfFolded(true);
        }
        if (info.getType().equals(HighlightInfoType.WRONG_REF)) {
            // when typing right after/before the unresolved identifier, its color must stay red
            highlighter.setGreedyToRight(true);
            highlighter.setGreedyToLeft(true);
        }
    }
}
