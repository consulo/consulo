// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.internal.SweepProcessor;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.RedBlackTreeVerifier;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.HighlightersRecycler;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UpdateHighlightersUtilImpl {
    private static boolean isCoveredByOffsets(HighlightInfo info, HighlightInfo coveredBy) {
        return coveredBy.getStartOffset() <= info.getStartOffset()
            && info.getEndOffset() <= coveredBy.getEndOffset()
            && info.getGutterIconRenderer() == null;
    }

    @RequiredUIAccess
    public static void addHighlighterToEditorIncrementally(
        @Nonnull Project project,
        @Nonnull Document document,
        @Nonnull PsiFile file,
        int startOffset,
        int endOffset,
        @Nonnull HighlightInfoImpl info,
        @Nullable EditorColorsScheme colorsScheme,
        // if null global scheme will be used
        int group,
        @Nonnull Map<TextRange, RangeMarker> ranges2markersCache
    ) {
        UIAccess.assertIsUIThread();
        if (UpdateHighlightersUtil.isFileLevelOrGutterAnnotation(info)) {
            return;
        }
        if (info.getStartOffset() < startOffset || info.getEndOffset() > endOffset) {
            return;
        }

        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        boolean myInfoIsError = isSevere(info, severityRegistrar);
        Predicate<HighlightInfo> otherHighlightInTheWayProcessor = oldInfo -> {
            if (!myInfoIsError && isCovered(info, severityRegistrar, oldInfo)) {
                return false;
            }

            return ((HighlightInfoImpl) oldInfo).getGroup() != group || !((HighlightInfoImpl) oldInfo).equalsByActualOffset(info);
        };
        boolean allIsClear = DaemonCodeAnalyzerInternal.processHighlights(
            document,
            project,
            null,
            info.getActualStartOffset(),
            info.getActualEndOffset(),
            otherHighlightInTheWayProcessor
        );
        if (allIsClear) {
            createOrReuseHighlighterFor(
                info,
                colorsScheme,
                document,
                group,
                file,
                (MarkupModelEx) markup,
                null,
                ranges2markersCache,
                severityRegistrar
            );

            clearWhiteSpaceOptimizationFlag(document);
            assertMarkupConsistent(markup, project);
        }
    }

    private static final Comparator<HighlightInfo> BY_START_OFFSET_NODUPS = (t1, t2) -> {
        HighlightInfoImpl o1 = (HighlightInfoImpl) t1;
        HighlightInfoImpl o2 = (HighlightInfoImpl) t2;

        int d = o1.getActualStartOffset() - o2.getActualStartOffset();
        if (d != 0) {
            return d;
        }
        d = o1.getActualEndOffset() - o2.getActualEndOffset();
        if (d != 0) {
            return d;
        }

        d = Comparing.compare(o1.getSeverity(), o2.getSeverity());
        if (d != 0) {
            return -d; // higher severity first, to prevent warnings overlap errors
        }

        if (!Objects.equals(o1.getType(), o2.getType())) {
            return String.valueOf(o1.getType()).compareTo(String.valueOf(o2.getType()));
        }

        if (!Objects.equals(o1.getGutterIconRenderer(), o2.getGutterIconRenderer())) {
            return String.valueOf(o1.getGutterIconRenderer()).compareTo(String.valueOf(o2.getGutterIconRenderer()));
        }

        if (!Objects.equals(o1.myForcedTextAttributes, o2.myForcedTextAttributes)) {
            return String.valueOf(o1.getGutterIconRenderer()).compareTo(String.valueOf(o2.getGutterIconRenderer()));
        }

        if (!Objects.equals(o1.myForcedTextAttributesKey, o2.myForcedTextAttributesKey)) {
            return String.valueOf(o1.getGutterIconRenderer()).compareTo(String.valueOf(o2.getGutterIconRenderer()));
        }

        return o1.getDescription().compareTo(o2.getDescription());
    };

    @RequiredUIAccess
    public static void setHighlightersInRange(
        @Nonnull Project project,
        @Nonnull Document document,
        @Nonnull TextRange range,
        @Nullable EditorColorsScheme colorsScheme,
        // if null global scheme will be used
        @Nonnull List<? extends HighlightInfo> infos,
        @Nonnull MarkupModelEx markup,
        int group
    ) {
        UIAccess.assertIsUIThread();

        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        HighlightersRecycler infosToRemove = new HighlightersRecycler();
        DaemonCodeAnalyzerInternal.processHighlights(
            document,
            project,
            null,
            range.getStartOffset(),
            range.getEndOffset(),
            i -> {
                HighlightInfoImpl info = (HighlightInfoImpl) i;
                if (info.getGroup() == group) {
                    RangeHighlighter highlighter = info.getHighlighter();
                    int hiStart = highlighter.getStartOffset();
                    int hiEnd = highlighter.getEndOffset();
                    boolean willBeRemoved = hiEnd == document.getTextLength() && range.getEndOffset() == document.getTextLength()
                        /*|| range.intersectsStrict(hiStart, hiEnd)*/
                        || range.containsRange(hiStart, hiEnd)
                        /*|| hiStart <= range.getStartOffset() && hiEnd >= range.getEndOffset()*/;
                    if (willBeRemoved) {
                        infosToRemove.recycleHighlighter(highlighter);
                        info.setHighlighter(null);
                    }
                }
                return true;
            }
        );

        Lists.quickSort(infos, BY_START_OFFSET_NODUPS);
        Map<TextRange, RangeMarker> ranges2markersCache = new HashMap<>(10);
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        DaemonCodeAnalyzerInternal codeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(project);
        boolean[] changed = {false};
        SweepProcessor.Generator<HighlightInfo> generator = (Predicate<HighlightInfo> processor) -> ContainerUtil.process(infos, processor);
        SweepProcessor.sweep(
            generator,
            (offset, i, atStart, overlappingIntervals) -> {
                if (!atStart) {
                    return true;
                }
                HighlightInfoImpl info = (HighlightInfoImpl) i;
                if (info.isFileLevelAnnotation() && psiFile != null && psiFile.getViewProvider().isPhysical()) {
                    codeAnalyzer.addFileLevelHighlight(project, group, info, psiFile);
                    changed[0] = true;
                    return true;
                }
                if (isWarningCoveredByError(info, overlappingIntervals, severityRegistrar)) {
                    return true;
                }
                if (info.getStartOffset() >= range.getStartOffset() && info.getEndOffset() <= range.getEndOffset() && psiFile != null) {
                    createOrReuseHighlighterFor(
                        info,
                        colorsScheme,
                        document,
                        group,
                        psiFile,
                        markup,
                        infosToRemove,
                        ranges2markersCache,
                        severityRegistrar
                    );
                    changed[0] = true;
                }
                return true;
            }
        );
        for (RangeHighlighter highlighter : infosToRemove.forAllInGarbageBin()) {
            highlighter.dispose();
            changed[0] = true;
        }

        if (changed[0]) {
            clearWhiteSpaceOptimizationFlag(document);
        }
        assertMarkupConsistent(markup, project);
    }

    private static boolean isWarningCoveredByError(
        @Nonnull HighlightInfo info,
        @Nonnull Collection<HighlightInfo> overlappingIntervals,
        @Nonnull SeverityRegistrar severityRegistrar
    ) {
        if (!isSevere(info, severityRegistrar)) {
            for (HighlightInfo overlapping : overlappingIntervals) {
                if (isCovered(info, severityRegistrar, overlapping)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCovered(
        @Nonnull HighlightInfo warning,
        @Nonnull SeverityRegistrar severityRegistrar,
        @Nonnull HighlightInfo candidate
    ) {
        if (!isCoveredByOffsets(warning, candidate)) {
            return false;
        }
        HighlightSeverity severity = candidate.getSeverity();
        if (severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY) {
            return false; // syntax should not interfere with warnings
        }
        return isSevere(candidate, severityRegistrar);
    }

    private static boolean isSevere(@Nonnull HighlightInfo info, @Nonnull SeverityRegistrar severityRegistrar) {
        HighlightSeverity severity = info.getSeverity();
        return severityRegistrar.compare(HighlightSeverity.ERROR, severity) <= 0 || severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY;
    }

    private static void createOrReuseHighlighterFor(
        @Nonnull HighlightInfoImpl info,
        @Nullable EditorColorsScheme colorsScheme,
        // if null global scheme will be used
        @Nonnull Document document,
        int group,
        @Nonnull PsiFile psiFile,
        @Nonnull MarkupModelEx markup,
        @Nullable HighlightersRecycler infosToRemove,
        @Nonnull Map<TextRange, RangeMarker> ranges2markersCache,
        @Nonnull SeverityRegistrar severityRegistrar
    ) {
        int infoStartOffset = info.getStartOffset();
        int infoEndOffset = info.getEndOffset();

        int docLength = document.getTextLength();
        if (infoEndOffset > docLength) {
            infoEndOffset = docLength;
            infoStartOffset = Math.min(infoStartOffset, infoEndOffset);
        }
        if (infoEndOffset == infoStartOffset && !info.isAfterEndOfLine()) {
            if (infoEndOffset == docLength) {
                return;  // empty highlighter beyond file boundaries
            }
            infoEndOffset++; //show something in case of empty HighlightInfo
        }

        info.setGroup(group);

        int layer = getLayer(info, severityRegistrar);
        RangeHighlighterEx highlighter = infosToRemove == null ? null : (RangeHighlighterEx) infosToRemove.pickupHighlighterFromGarbageBin(
            infoStartOffset,
            infoEndOffset,
            layer
        );

        TextRange finalInfoRange = new TextRange(infoStartOffset, infoEndOffset);
        TextAttributes infoAttributes = info.getTextAttributes(psiFile, colorsScheme);
        @RequiredUIAccess
        Consumer<RangeHighlighterEx> changeAttributes = finalHighlighter -> {
            if (infoAttributes != null) {
                finalHighlighter.setTextAttributes(infoAttributes);
            }

            info.setHighlighter(finalHighlighter);
            finalHighlighter.setAfterEndOfLine(info.isAfterEndOfLine());

            ColorValue color = info.getErrorStripeMarkColor(psiFile, colorsScheme);
            finalHighlighter.setErrorStripeMarkColor(color);
            if (info != finalHighlighter.getErrorStripeTooltip()) {
                finalHighlighter.setErrorStripeTooltip(info);
            }
            GutterMark renderer = info.getGutterIconRenderer();
            finalHighlighter.setGutterIconRenderer((GutterIconRenderer) renderer);

            ranges2markersCache.put(finalInfoRange, info.getHighlighter());
            List<Pair<IntentionActionDescriptor, TextRange>> quickFixActionRanges = info.myQuickFixActionRanges;
            if (quickFixActionRanges != null) {
                List<Pair<IntentionActionDescriptor, RangeMarker>> list = new ArrayList<>(quickFixActionRanges.size());
                for (Pair<IntentionActionDescriptor, TextRange> pair : quickFixActionRanges) {
                    TextRange textRange = pair.second;
                    RangeMarker marker = getOrCreate(document, ranges2markersCache, textRange);
                    list.add(Pair.create(pair.first, marker));
                }
                info.myQuickFixActionMarkers = Lists.newLockFreeCopyOnWriteList(list);
            }
            ProperTextRange fixRange = info.getFixTextRange();
            if (finalInfoRange.equals(fixRange)) {
                info.myFixMarker = null; // null means it the same as highlighter's
            }
            else {
                info.myFixMarker = getOrCreate(document, ranges2markersCache, fixRange);
            }
        };

        if (highlighter == null) {
            highlighter = markup.addRangeHighlighterAndChangeAttributes(
                null,
                infoStartOffset,
                infoEndOffset,
                layer,
                HighlighterTargetArea.EXACT_RANGE,
                false,
                changeAttributes
            );
            if (HighlightInfoType.VISIBLE_IF_FOLDED.contains(info.getType())) {
                highlighter.setVisibleIfFolded(true);
            }
        }
        else {
            markup.changeAttributesInBatch(highlighter, changeAttributes);
        }

        if (infoAttributes != null) {
            boolean attributesSet = Comparing.equal(infoAttributes, highlighter.getTextAttributes(colorsScheme));
            assert attributesSet
                : "Info: " + infoAttributes +
                    "; colorsScheme: " + (colorsScheme == null ? "[global]" : colorsScheme.getName()) +
                    "; highlighter:" + highlighter.getTextAttributes(colorsScheme);
        }
    }

    private static int getLayer(@Nonnull HighlightInfoImpl info, @Nonnull SeverityRegistrar severityRegistrar) {
        HighlightSeverity severity = info.getSeverity();
        int layer;
        if (severity == HighlightSeverity.WARNING) {
            layer = HighlighterLayer.WARNING;
        }
        else if (severity == HighlightSeverity.WEAK_WARNING) {
            layer = HighlighterLayer.WEAK_WARNING;
        }
        else if (severityRegistrar.compare(severity, HighlightSeverity.ERROR) >= 0) {
            layer = HighlighterLayer.ERROR;
        }
        else if (severity == HighlightInfoType.INJECTED_FRAGMENT_SEVERITY) {
            layer = HighlighterLayer.CARET_ROW - 1;
        }
        else if (severity == HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY) {
            layer = HighlighterLayer.ELEMENT_UNDER_CARET;
        }
        else {
            layer = HighlighterLayer.ADDITIONAL_SYNTAX;
        }
        return layer;
    }

    @Nonnull
    private static RangeMarker getOrCreate(
        @Nonnull Document document,
        @Nonnull Map<TextRange, RangeMarker> ranges2markersCache,
        @Nonnull TextRange textRange
    ) {
        return ranges2markersCache.computeIfAbsent(textRange, __ -> document.createRangeMarker(textRange));
    }

    private static final Key<Boolean> TYPING_INSIDE_HIGHLIGHTER_OCCURRED = Key.create("TYPING_INSIDE_HIGHLIGHTER_OCCURRED");

    public static boolean isWhitespaceOptimizationAllowed(@Nonnull Document document) {
        return document.getUserData(TYPING_INSIDE_HIGHLIGHTER_OCCURRED) == null;
    }

    private static void disableWhiteSpaceOptimization(@Nonnull Document document) {
        document.putUserData(TYPING_INSIDE_HIGHLIGHTER_OCCURRED, Boolean.TRUE);
    }

    private static void clearWhiteSpaceOptimizationFlag(@Nonnull Document document) {
        document.putUserData(TYPING_INSIDE_HIGHLIGHTER_OCCURRED, null);
    }

    @RequiredUIAccess
    public static void updateHighlightersByTyping(@Nonnull Project project, @Nonnull DocumentEvent e) {
        UIAccess.assertIsUIThread();

        Document document = e.getDocument();
        if (document instanceof DocumentEx && document.isInBulkUpdate()) {
            return;
        }

        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        assertMarkupConsistent(markup, project);

        int start = e.getOffset() - 1;
        int end = start + e.getOldLength();

        List<HighlightInfoImpl> toRemove = new ArrayList<>();
        DaemonCodeAnalyzerInternal.processHighlights(
            document,
            project,
            null,
            start,
            end,
            i -> {
                HighlightInfoImpl info = (HighlightInfoImpl) i;

                if (!info.needUpdateOnTyping()) {
                    return true;
                }

                RangeHighlighter highlighter = info.getHighlighter();
                int highlighterStart = highlighter.getStartOffset();
                int highlighterEnd = highlighter.getEndOffset();
                if (info.isAfterEndOfLine()) {
                    if (highlighterStart < document.getTextLength()) {
                        highlighterStart += 1;
                    }
                    if (highlighterEnd < document.getTextLength()) {
                        highlighterEnd += 1;
                    }
                }
                if (!highlighter.isValid() || start < highlighterEnd && highlighterStart <= end) {
                    toRemove.add(info);
                }
                return true;
            }
        );

        for (HighlightInfoImpl info : toRemove) {
            if (!info.getHighlighter().isValid() || info.getType().equals(HighlightInfoType.WRONG_REF)) {
                info.getHighlighter().dispose();
            }
        }

        assertMarkupConsistent(markup, project);

        if (!toRemove.isEmpty()) {
            disableWhiteSpaceOptimization(document);
        }
    }

    @RequiredReadAction
    public static void assertMarkupConsistent(@Nonnull MarkupModel markup, @Nonnull Project project) {
        if (!RedBlackTreeVerifier.VERIFY) {
            return;
        }
        Document document = markup.getDocument();
        DaemonCodeAnalyzerInternal.processHighlights(
            document,
            project,
            null,
            0,
            document.getTextLength(),
            info -> {
                assert ((MarkupModelEx) markup).containsHighlighter(info.getHighlighter());
                return true;
            }
        );
        RangeHighlighter[] allHighlighters = markup.getAllHighlighters();
        for (RangeHighlighter highlighter : allHighlighters) {
            if (!highlighter.isValid()) {
                continue;
            }
            HighlightInfoImpl info = HighlightInfoImpl.fromRangeHighlighter(highlighter);
            if (info == null) {
                continue;
            }
            boolean contains = !DaemonCodeAnalyzerInternal.processHighlights(
                document,
                project,
                null,
                info.getActualStartOffset(),
                info.getActualEndOffset(),
                highlightInfo -> BY_START_OFFSET_NODUPS.compare(highlightInfo, info) != 0
            );
            assert contains : info;
        }
    }


    // set highlights inside startOffset,endOffset but outside priorityRange
    @RequiredUIAccess
    static void setHighlightersOutsideRange(
        @Nonnull Project project,
        @Nonnull Document document,
        @Nonnull PsiFile psiFile,
        @Nonnull List<? extends HighlightInfo> infos,
        @Nullable EditorColorsScheme colorsScheme,
        // if null global scheme will be used
        int startOffset,
        int endOffset,
        @Nonnull ProperTextRange priorityRange,
        int group
    ) {
        UIAccess.assertIsUIThread();

        DaemonCodeAnalyzerInternal codeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(project);
        if (startOffset == 0 && endOffset == document.getTextLength()) {
            codeAnalyzer.cleanFileLevelHighlights(project, group, psiFile);
        }

        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        assertMarkupConsistent(markup, project);

        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        HighlightersRecycler infosToRemove = new HighlightersRecycler();
        Lists.quickSort(infos, BY_START_OFFSET_NODUPS);
        Set<HighlightInfo> infoSet = new HashSet<>(infos);

        @RequiredUIAccess
        Predicate<HighlightInfo> processor = info -> {
            HighlightInfoImpl highlightInfo = (HighlightInfoImpl) info;
            if (highlightInfo.getGroup() == group) {
                RangeHighlighter highlighter = info.getHighlighter();
                int hiStart = highlighter.getStartOffset();
                int hiEnd = highlighter.getEndOffset();
                if (!highlightInfo.isFromInjection()
                    && hiEnd < document.getTextLength()
                    && (hiEnd <= startOffset || hiStart >= endOffset)) {
                    return true; // injections are oblivious to restricting range
                }
                boolean toRemove = infoSet.contains(info)
                    || !priorityRange.containsRange(hiStart, hiEnd)
                    && (hiEnd != document.getTextLength() || priorityRange.getEndOffset() != document.getTextLength());
                if (toRemove) {
                    infosToRemove.recycleHighlighter(highlighter);
                    highlightInfo.setHighlighter(null);
                }
            }
            return true;
        };
        DaemonCodeAnalyzerInternal.processHighlightsOverlappingOutside(
            document,
            project,
            null,
            priorityRange.getStartOffset(),
            priorityRange.getEndOffset(),
            processor
        );

        Map<TextRange, RangeMarker> ranges2markersCache = new HashMap<>(10);
        boolean[] changed = {false};
        SweepProcessor.Generator<HighlightInfo> generator = proc -> ContainerUtil.process(infos, proc);
        SweepProcessor.sweep(
            generator,
            (offset, i, atStart, overlappingIntervals) -> {
                if (!atStart) {
                    return true;
                }
                HighlightInfoImpl info = (HighlightInfoImpl) i;

                if (!info.isFromInjection()
                    && info.getEndOffset() < document.getTextLength()
                    && (info.getEndOffset() <= startOffset || info.getStartOffset() >= endOffset)) {
                    return true; // injections are oblivious to restricting range
                }

                if (info.isFileLevelAnnotation()) {
                    codeAnalyzer.addFileLevelHighlight(project, group, info, psiFile);
                    changed[0] = true;
                    return true;
                }
                if (isWarningCoveredByError(info, overlappingIntervals, severityRegistrar)) {
                    return true;
                }
                if (info.getStartOffset() < priorityRange.getStartOffset() || info.getEndOffset() > priorityRange.getEndOffset()) {
                    createOrReuseHighlighterFor(
                        info,
                        colorsScheme,
                        document,
                        group,
                        psiFile,
                        (MarkupModelEx) markup,
                        infosToRemove,
                        ranges2markersCache,
                        severityRegistrar
                    );
                    changed[0] = true;
                }
                return true;
            }
        );
        for (RangeHighlighter highlighter : infosToRemove.forAllInGarbageBin()) {
            highlighter.dispose();
            changed[0] = true;
        }

        if (changed[0]) {
            clearWhiteSpaceOptimizationFlag(document);
        }
        assertMarkupConsistent(markup, project);
    }
}
