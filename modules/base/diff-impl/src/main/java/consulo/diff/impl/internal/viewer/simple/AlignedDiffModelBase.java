// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.viewer.simple;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.ComponentInlayAlignment;
import consulo.codeEditor.ComponentInlayRenderer;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayModel;
import consulo.codeEditor.InlayProperties;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.diff.DiffContext;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.Range;
import consulo.diff.util.Side;
import consulo.diff.util.TextDiffType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.IntBinaryOperator;

/**
 * WIP:
 * <p>
 * Benefits of the new model:
 * * Alignment of one change never affects alignment of others (except foldings)
 * <p>
 * Current problems:
 * * When changes follow one by one ("Highlight split changes"-mode), some inlays may be mixed up. The root of the problem is that
 * several inlays with the same priority added to the same offset
 * * Mirror inlays positioning works not so good
 */
public abstract class AlignedDiffModelBase implements AlignedDiffModel {
    private static final long REALIGN_DELAY_MS = 300;

    private final DiffRequest myDiffRequest;
    private final DiffContext myDiffContext;
    private final EditorEx myEditor1;
    private final EditorEx myEditor2;
    private final IntBinaryOperator myLineTransfer;

    // sorted by highlighter offset
    private final List<ChangeInlay> myChangeInlays = new ArrayList<>();
    private final List<MirrorInlay> myMirrorInlays1 = new ArrayList<>();
    private final List<MirrorInlay> myMirrorInlays2 = new ArrayList<>();

    private final List<RangeHighlighter> myRangeHighlighters1 = new ArrayList<>();
    private final List<RangeHighlighter> myRangeHighlighters2 = new ArrayList<>();

    private int myRealignGeneration;
    private boolean myRealigning;
    private boolean myDisposed;

    public AlignedDiffModelBase(
        DiffRequest diffRequest,
        DiffContext diffContext,
        EditorEx editor1,
        EditorEx editor2,
        IntBinaryOperator lineTransfer
    ) {
        myDiffRequest = diffRequest;
        myDiffContext = diffContext;
        myEditor1 = editor1;
        myEditor2 = editor2;
        myLineTransfer = lineTransfer;

        MyInlayModelListener inlayListener = new MyInlayModelListener();
        editor1.getInlayModel().addListener(inlayListener, this);
        editor2.getInlayModel().addListener(inlayListener, this);
    }

    protected TextDiffSettingsHolder.TextDiffSettings getTextSettings() {
        return TextDiffViewerUtil.getTextSettings(myDiffContext);
    }

    @RequiredUIAccess
    public void scheduleRealignChanges() {
        if (!needAlignChanges()) {
            return;
        }
        int generation = ++myRealignGeneration;
        UIAccess uiAccess = UIAccess.current();
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> uiAccess.execute(() -> {
            if (!myDisposed && generation == myRealignGeneration) {
                realignChanges();
            }
        }), REALIGN_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean needAlignChanges() {
        Boolean forcedValue = myDiffRequest.getUserData(DiffUserDataKeys.ALIGNED_TWO_SIDED_DIFF);
        if (forcedValue == null) {
            forcedValue = myDiffContext.getUserData(DiffUserDataKeys.ALIGNED_TWO_SIDED_DIFF);
        }
        if (forcedValue != null) {
            return forcedValue;
        }

        return getTextSettings().isEnableAligningChangesMode();
    }

    @RequiredUIAccess
    @Override
    public void realignChanges() {
        if (!needAlignChanges()) {
            return;
        }
        if (myEditor1.isDisposed() || myEditor2.isDisposed() || myRealigning) {
            return;
        }

        myRealigning = true;
        try {
            clear();

            initInlays();
            updateInlayHeights();
        }
        finally {
            myRealigning = false;
        }
    }

    private static int calcPriority(boolean isLastLine, boolean isTop) {
        if (isLastLine) {
            return isTop ? Integer.MAX_VALUE : Integer.MAX_VALUE - 1;
        }
        else {
            return isTop ? Integer.MAX_VALUE - 1 : Integer.MAX_VALUE;
        }
    }

    @RequiredUIAccess
    private void initInlays() {
        TreeMap<Integer, ChangeInlay> map1 = new TreeMap<>();
        TreeMap<Integer, ChangeInlay> map2 = new TreeMap<>();

        for (AlignableChange change : getDiffChanges()) {
            Range range = getRange(change);

            ChangeInlay changeInlay = new ChangeInlay(
                change,
                createAlignInlay(Side.LEFT, range.start1(), true, change.getDiffType()),
                createAlignInlay(Side.RIGHT, range.start2(), true, change.getDiffType()),
                createAlignInlay(Side.LEFT, range.end1(), false, change.getDiffType()),
                createAlignInlay(Side.RIGHT, range.end2(), false, change.getDiffType())
            );

            myChangeInlays.add(changeInlay);
            createGutterHighlighterIfNeeded(change.getDiffType(), Side.LEFT, changeInlay.myBottomInlay1);
            createGutterHighlighterIfNeeded(change.getDiffType(), Side.RIGHT, changeInlay.myBottomInlay2);
            map1.put(range.start1(), changeInlay);
            map2.put(range.start2(), changeInlay);
        }

        for (Inlay<?> sourceInlay : myEditor1.getInlayModel().getBlockElementsInRange(0, myEditor1.getDocument().getTextLength())) {
            if (isAlignInlay(sourceInlay)) {
                continue;
            }
            int targetLine = getMirrorTargetLine(Side.LEFT, sourceInlay);
            ChangeInlay changeBlock = getChangeBlockFor(Side.LEFT, map1, targetLine);
            if (changeBlock != null) {
                changeBlock.myInnerSourceInlay1.add(sourceInlay);
            }
            else {
                myMirrorInlays1.add(new MirrorInlay(sourceInlay, createMirrorInlay(Side.LEFT, sourceInlay, targetLine)));
            }
        }

        for (Inlay<?> sourceInlay : myEditor2.getInlayModel().getBlockElementsInRange(0, myEditor2.getDocument().getTextLength())) {
            if (isAlignInlay(sourceInlay)) {
                continue;
            }
            int targetLine = getMirrorTargetLine(Side.RIGHT, sourceInlay);
            ChangeInlay changeBlock = getChangeBlockFor(Side.RIGHT, map2, targetLine);
            if (changeBlock != null) {
                changeBlock.myInnerSourceInlay2.add(sourceInlay);
            }
            else {
                myMirrorInlays2.add(new MirrorInlay(sourceInlay, createMirrorInlay(Side.RIGHT, sourceInlay, targetLine)));
            }
        }
    }

    @RequiredUIAccess
    private @Nullable AlignInlay createAlignInlay(Side side, int line, boolean isTop, TextDiffType diffType) {
        EditorEx editor = getEditor(side);
        boolean isLastLine = line == DiffImplUtil.getLineCount(editor.getDocument());
        int offset = DiffImplUtil.getOffset(editor.getDocument(), line, 0);

        InlayProperties properties = new InlayProperties()
            .showAbove(!isLastLine)
            .priority(calcPriority(isLastLine, isTop));

        ColorValue color = isTop ? null : AlignableChange.getAlignedChangeColor(diffType, editor);
        return AlignInlay.create(editor, offset, properties, color);
    }

    @RequiredUIAccess
    private void createGutterHighlighterIfNeeded(TextDiffType diffType, Side side, @Nullable AlignInlay inlay) {
        if (inlay == null || diffType == TextDiffType.MODIFIED) {
            return;
        }
        EditorEx editor = getEditor(side);
        int offset = inlay.myInlay.getOffset();
        RangeHighlighter highlighter = editor.getMarkupModel()
            .addRangeHighlighter(offset, offset, HighlighterLayer.SELECTION, new TextAttributes(), HighlighterTargetArea.EXACT_RANGE);
        highlighter.setLineMarkerPresentationProvider(new DiffInlayGutterMarkerRenderer(diffType, editor, inlay.myInlay));
        if (side == Side.LEFT) {
            myRangeHighlighters1.add(highlighter);
        }
        else {
            myRangeHighlighters2.add(highlighter);
        }
    }

    private EditorEx getEditor(Side side) {
        return side.selectNotNull(myEditor1, myEditor2);
    }

    @RequiredUIAccess
    private @Nullable AlignInlay createMirrorInlay(Side sourceSide, Inlay<?> sourceInlay, int targetLine) {
        Side side = sourceSide.other();
        EditorEx editor = getEditor(side);
        int offset = DiffImplUtil.getOffset(editor.getDocument(), targetLine, 0);

        InlayProperties inlayProperties = new InlayProperties()
            .showAbove(sourceInlay.getProperties().isShownAbove())
            .priority(sourceInlay.getProperties().getPriority());

        return AlignInlay.create(editor, offset, inlayProperties, null);
    }

    private int getMirrorTargetLine(Side sourceSide, Inlay<?> sourceInlay) {
        EditorEx sourceEditor = getEditor(sourceSide);

        int sourceLine = sourceEditor.offsetToLogicalPosition(sourceInlay.getOffset()).line;
        return myLineTransfer.applyAsInt(sourceSide.getIndex(), sourceLine);
    }

    private @Nullable ChangeInlay getChangeBlockFor(Side sourceSide, TreeMap<Integer, ChangeInlay> changeBlockMap, int line) {
        Map.Entry<Integer, ChangeInlay> entry = changeBlockMap.ceilingEntry(line);
        if (entry == null) {
            return null;
        }
        ChangeInlay block = entry.getValue();
        Range range = getRange(block.myChange);
        if (start(range, sourceSide) <= line && end(range, sourceSide) > line) {
            return block;
        }
        return null;
    }

    @RequiredUIAccess
    private void updateInlayHeights() {
        int last1 = 0;
        int last2 = 0;
        for (ChangeInlay changeInlay : myChangeInlays) {
            Range range = getRange(changeInlay.myChange);

            int prefixDelta = calcEffectiveVisualLinesHeight(myEditor2, last2, range.start2())
                - calcEffectiveVisualLinesHeight(myEditor1, last1, range.start1());

            if (prefixDelta >= 0) {
                changeInlay.setTop(prefixDelta, 0);
            }
            else {
                changeInlay.setTop(0, -prefixDelta);
            }

            int bodyDelta = calcEffectiveVisualLinesHeight(myEditor2, range.start2(), range.end2())
                - calcEffectiveVisualLinesHeight(myEditor1, range.start1(), range.end1())
                + sumHeights(changeInlay.myInnerSourceInlay2)
                - sumHeights(changeInlay.myInnerSourceInlay1);
            if (bodyDelta >= 0) {
                changeInlay.setBottom(bodyDelta, 0);
            }
            else {
                changeInlay.setBottom(0, -bodyDelta);
            }

            last1 = range.end1();
            last2 = range.end2();
        }

        for (MirrorInlay mirrorInlay : myMirrorInlays1) {
            if (mirrorInlay.myInlay != null) {
                mirrorInlay.myInlay.setHeight(mirrorInlay.mySourceInlay.getHeightInPixels());
            }
        }
        for (MirrorInlay mirrorInlay : myMirrorInlays2) {
            if (mirrorInlay.myInlay != null) {
                mirrorInlay.myInlay.setHeight(mirrorInlay.mySourceInlay.getHeightInPixels());
            }
        }
    }

    private static int sumHeights(List<Inlay<?>> inlays) {
        int sum = 0;
        for (Inlay<?> inlay : inlays) {
            sum += inlay.getHeightInPixels();
        }
        return sum;
    }

    /**
     * Make adjustments to SoftWraps and folded lines
     */
    private static int calcEffectiveVisualLinesHeight(Editor editor, int startLine, int endLine) {
        if (startLine == endLine) {
            return 0;
        }

        Document document = editor.getDocument();
        int lineCount = document.getLineCount();
        if (lineCount == 0) {
            return 0;
        }

        int vLine1 = editor.offsetToVisualPosition(document.getLineStartOffset(startLine), false, false).line;
        int vLine2 = endLine == lineCount
            ? editor.offsetToVisualPosition(document.getLineStartOffset(endLine - 1), false, false).line + 1
            : editor.offsetToVisualPosition(document.getLineStartOffset(endLine), false, false).line;
        return editor.getLineHeight() * (vLine2 - vLine1);
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (ChangeInlay inlay : myChangeInlays) {
            inlay.dispose();
        }
        myChangeInlays.clear();
        for (MirrorInlay inlay : myMirrorInlays1) {
            inlay.dispose();
        }
        myMirrorInlays1.clear();
        for (MirrorInlay inlay : myMirrorInlays2) {
            inlay.dispose();
        }
        myMirrorInlays2.clear();

        for (RangeHighlighter highlighter : myRangeHighlighters1) {
            myEditor1.getMarkupModel().removeHighlighter(highlighter);
        }
        myRangeHighlighters1.clear();

        for (RangeHighlighter highlighter : myRangeHighlighters2) {
            myEditor2.getMarkupModel().removeHighlighter(highlighter);
        }
        myRangeHighlighters2.clear();
    }

    private static Range getRange(AlignableChange change) {
        return new Range(
            change.getStartLine(Side.LEFT),
            change.getEndLine(Side.LEFT),
            change.getStartLine(Side.RIGHT),
            change.getEndLine(Side.RIGHT)
        );
    }

    private static int start(Range range, Side side) {
        return side.isLeft() ? range.start1() : range.start2();
    }

    private static int end(Range range, Side side) {
        return side.isLeft() ? range.end1() : range.end2();
    }

    private static boolean isAlignInlay(Inlay<?> inlay) {
        return inlay.getRenderer() instanceof AlignInlay.Renderer;
    }

    private class MyInlayModelListener implements InlayModel.Listener {
        @Override
        public void onAdded(Inlay<?> inlay) {
            if (isAlignInlay(inlay) || myRealigning) {
                return;
            }
            scheduleRealignChanges();
        }

        @Override
        public void onRemoved(Inlay<?> inlay) {
            if (isAlignInlay(inlay) || myRealigning) {
                return;
            }
            scheduleRealignChanges();
        }

        @Override
        public void onUpdated(Inlay<?> inlay, int changeFlags) {
            if (isAlignInlay(inlay) || myRealigning) {
                return;
            }
            if ((changeFlags & InlayModel.ChangeFlags.HEIGHT_CHANGED) != 0) {
                scheduleRealignChanges();
            }
        }
    }

    private static final class ChangeInlay implements Disposable {
        private final AlignableChange myChange;

        private final @Nullable AlignInlay myTopInlay1;
        private final @Nullable AlignInlay myTopInlay2;

        private final @Nullable AlignInlay myBottomInlay1;
        private final @Nullable AlignInlay myBottomInlay2;

        private final List<Inlay<?>> myInnerSourceInlay1 = new ArrayList<>();
        private final List<Inlay<?>> myInnerSourceInlay2 = new ArrayList<>();

        private ChangeInlay(
            AlignableChange change,
            @Nullable AlignInlay topInlay1,
            @Nullable AlignInlay topInlay2,
            @Nullable AlignInlay bottomInlay1,
            @Nullable AlignInlay bottomInlay2
        ) {
            myChange = change;
            myTopInlay1 = topInlay1;
            myTopInlay2 = topInlay2;
            myBottomInlay1 = bottomInlay1;
            myBottomInlay2 = bottomInlay2;
        }

        @RequiredUIAccess
        private void setTop(int height1, int height2) {
            if (myTopInlay1 != null) {
                myTopInlay1.setHeight(height1);
            }
            if (myTopInlay2 != null) {
                myTopInlay2.setHeight(height2);
            }
        }

        @RequiredUIAccess
        private void setBottom(int height1, int height2) {
            if (myBottomInlay1 != null) {
                myBottomInlay1.setHeight(height1);
            }
            if (myBottomInlay2 != null) {
                myBottomInlay2.setHeight(height2);
            }
        }

        @Override
        public void dispose() {
            for (AlignInlay inlay : new AlignInlay[]{myTopInlay1, myTopInlay2, myBottomInlay1, myBottomInlay2}) {
                if (inlay != null) {
                    inlay.dispose();
                }
            }
        }
    }

    private static final class MirrorInlay implements Disposable {
        private final Inlay<?> mySourceInlay;
        private final @Nullable AlignInlay myInlay;

        private MirrorInlay(Inlay<?> sourceInlay, @Nullable AlignInlay inlay) {
            mySourceInlay = sourceInlay;
            myInlay = inlay;
        }

        @Override
        public void dispose() {
            if (myInlay != null) {
                myInlay.dispose();
            }
        }
    }

    private static final class AlignInlay implements Disposable {
        private static final class Renderer extends ComponentInlayRenderer {
            private Renderer(VerticalLayout component) {
                super(component, ComponentInlayAlignment.STRETCH_TO_CONTENT_WIDTH);
            }
        }

        private final Inlay<Renderer> myInlay;
        private final VerticalLayout mySpacer;
        private int myHeight = -1;

        private AlignInlay(Inlay<Renderer> inlay, VerticalLayout spacer) {
            myInlay = inlay;
            mySpacer = spacer;
        }

        @RequiredUIAccess
        private static @Nullable AlignInlay create(EditorEx editor, int offset, InlayProperties properties, @Nullable ColorValue color) {
            VerticalLayout spacer = VerticalLayout.create();
            spacer.setHeight(0);
            if (color != null) {
                spacer.setBackgroundColor(color);
            }
            Inlay<Renderer> inlay = editor.getInlayModel().addBlockElement(offset, properties, new Renderer(spacer));
            return inlay == null ? null : new AlignInlay(inlay, spacer);
        }

        @RequiredUIAccess
        private void setHeight(int height) {
            if (myHeight == height) {
                return;
            }
            myHeight = height;
            mySpacer.setHeight(height);
            myInlay.update();
        }

        @Override
        public void dispose() {
            Disposer.dispose(myInlay);
        }
    }
}
