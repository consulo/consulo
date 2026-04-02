// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.timeline;

import com.intellij.collaboration.async.LaunchNowKt;
import com.intellij.collaboration.ui.ClippingRoundedPanel;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation;
import com.intellij.collaboration.ui.util.BindChildInKt;
import com.intellij.collaboration.ui.util.BindVisibilityInKt;
import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.diff.util.LineRange;
import consulo.diff.util.TextDiffType;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SideBorder;
import consulo.util.io.PathUtil;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Function;

public final class TimelineDiffComponentFactory {
    private TimelineDiffComponentFactory() {
    }

    public static @Nonnull JComponent createDiffComponentIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Project project,
        @Nonnull EditorFactory editorFactory,
        @Nonnull PatchHunk patchHunk,
        @Nonnull DiffLineLocation anchor,
        @Nullable DiffLineLocation anchorStart
    ) {
        PatchHunk truncatedHunk = truncateHunk(patchHunk, anchor, anchorStart);

        Integer anchorLineIndex = PatchHunkUtil.findHunkLineIndex(truncatedHunk, anchor);
        Integer anchorStartLineIndex = anchorStart != null && !anchorStart.equals(anchor)
            ? PatchHunkUtil.findHunkLineIndex(truncatedHunk, anchorStart) : null;
        LineRange anchorRange;
        if (anchorLineIndex == null) {
            anchorRange = null;
        }
        else if (anchorStartLineIndex != null) {
            anchorRange = new LineRange(anchorStartLineIndex, anchorLineIndex + 1);
        }
        else {
            anchorRange = new LineRange(anchorLineIndex, anchorLineIndex + 1);
        }

        return createDiffComponentIn(cs, project, editorFactory, truncatedHunk, anchorRange);
    }

    public static @Nonnull JComponent createDiffComponentIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Project project,
        @Nonnull EditorFactory editorFactory,
        @Nonnull PatchHunk patchHunk,
        @Nullable LineRange anchorLineRange
    ) {
        if (patchHunk.getLines().stream().anyMatch(line -> line.getType() != PatchLine.Type.CONTEXT)) {
            List<AppliedTextPatch.AppliedSplitPatchHunk> appliedSplitHunks =
                GenericPatchApplier.SplitHunk.read(patchHunk).stream()
                    .map(it -> new AppliedTextPatch.AppliedSplitPatchHunk(it, -1, -1, AppliedTextPatch.HunkStatus.NOT_APPLIED))
                    .toList();

            var state = new PatchChangeBuilder().buildFromApplied(appliedSplitHunks);
            String patchContent = state.getPatchContent();
            if (patchContent.endsWith("\n")) {
                patchContent = patchContent.substring(0, patchContent.length() - 1);
            }

            EditorEx editor = (EditorEx) createDiffEditorIn(cs, project, editorFactory, patchContent);
            editor.getGutter().setLineNumberConverter(
                new LineNumberConverterAdapter(state.getLineConvertor1().createConvertor()),
                new LineNumberConverterAdapter(state.getLineConvertor2().createConvertor())
            );

            for (var hunk : state.getHunks()) {
                DiffDrawUtil.createUnifiedChunkHighlighters(editor, hunk.getPatchDeletionRange(), hunk.getPatchInsertionRange(), null);
            }
            if (anchorLineRange != null) {
                highlightAnchor(editor, anchorLineRange);
            }
            return editor.getComponent();
        }
        else {
            String patchContent = patchHunk.getText();
            if (patchContent.endsWith("\n")) {
                patchContent = patchContent.substring(0, patchContent.length() - 1);
            }

            EditorEx editor = (EditorEx) createDiffEditorIn(cs, project, editorFactory, patchContent);
            int startBefore = patchHunk.getStartLineBefore();
            int startAfter = patchHunk.getStartLineAfter();
            editor.getGutter().setLineNumberConverter(
                LineNumberConverter.Increasing.build((editorRef, line) -> line + startBefore),
                LineNumberConverter.Increasing.build((editorRef, line) -> line + startAfter)
            );
            if (anchorLineRange != null) {
                highlightAnchor(editor, anchorLineRange);
            }
            return editor.getComponent();
        }
    }

    private static void highlightAnchor(@Nonnull Editor editor, @Nonnull LineRange lineRange) {
        DiffDrawUtil.createHighlighter(editor, lineRange.start, lineRange.end, AnchorLine.INSTANCE, false);
    }

    @ApiStatus.Internal
    public static final class AnchorLine implements TextDiffType {
        public static final AnchorLine INSTANCE = new AnchorLine();

        private AnchorLine() {
        }

        @Override
        public @Nonnull String getName() {
            return "Comment Anchor Line";
        }

        @Override
        public @Nonnull Color getColor(@Nullable Editor editor) {
            return JBColor.namedColor(
                "Review.Timeline.Thread.Diff.AnchorLine",
                new JBColor(0xFBF1D1, 0x544B2D)
            );
        }

        @Override
        public @Nonnull Color getIgnoredColor(@Nullable Editor editor) {
            return getColor(editor);
        }

        @Override
        public @Nonnull Color getMarkerColor(@Nullable Editor editor) {
            return getColor(editor);
        }
    }

    @ApiStatus.Internal
    public static final int DIFF_CONTEXT_SIZE = 3;

    private static @Nonnull PatchHunk truncateHunk(
        @Nonnull PatchHunk hunk,
        @Nonnull DiffLineLocation anchor,
        @Nullable DiffLineLocation anchorStart
    ) {
        if (hunk.getLines().size() <= DIFF_CONTEXT_SIZE + 1) {
            return hunk;
        }
        DiffLineLocation actualAnchorStart = anchorStart != null && !anchorStart.equals(anchor) ? anchorStart : anchor;
        return truncateHunkAfter(truncateHunkBefore(hunk, actualAnchorStart), anchor);
    }

    private static @Nonnull PatchHunk truncateHunkBefore(@Nonnull PatchHunk hunk, @Nonnull DiffLineLocation location) {
        if (hunk.getLines().size() <= DIFF_CONTEXT_SIZE + 1) {
            return hunk;
        }
        Integer lineIdx = PatchHunkUtil.findHunkLineIndex(hunk, location);
        if (lineIdx == null) {
            return hunk;
        }
        int startIdx = lineIdx - DIFF_CONTEXT_SIZE;
        return PatchHunkUtil.truncateHunkBefore(hunk, startIdx);
    }

    private static @Nonnull PatchHunk truncateHunkAfter(@Nonnull PatchHunk hunk, @Nonnull DiffLineLocation location) {
        if (hunk.getLines().size() <= DIFF_CONTEXT_SIZE + 1) {
            return hunk;
        }
        Integer lineIdx = PatchHunkUtil.findHunkLineIndex(hunk, location);
        if (lineIdx == null) {
            return hunk;
        }
        int endIdx = lineIdx + DIFF_CONTEXT_SIZE;
        return PatchHunkUtil.truncateHunkAfter(hunk, endIdx);
    }

    public static @Nonnull Editor createDiffEditorIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Project project,
        @Nonnull EditorFactory editorFactory,
        @Nonnull CharSequence text
    ) {
        var document = editorFactory.createDocument(text);
        EditorEx editor = (EditorEx) editorFactory.createViewer(document, project, EditorKind.DIFF);
        editor.getGutterComponentEx().setPaintBackground(false);

        editor.setRendererMode(true);
        editor.setHorizontalScrollbarVisible(true);
        editor.setVerticalScrollbarVisible(false);
        editor.setCaretEnabled(false);
        editor.setEmbeddedIntoDialogWrapper(true);
        editor.getContentComponent().setOpaque(false);

        editor.setBorder(JBUI.Borders.empty());

        var settings = editor.getSettings();
        settings.setShowIntentionBulb(false);
        settings.setCaretRowShown(false);
        settings.setAdditionalLinesCount(0);
        settings.setAdditionalColumnsCount(0);
        settings.setRightMarginShown(false);
        settings.setRightMargin(-1);
        settings.setFoldingOutlineShown(false);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);
        settings.setAdditionalPageAtBottom(false);
        settings.setLineCursorWidth(1);

        LaunchNowKt.launchNow(
            cs,
            (scope, cont) -> {
                try {
                    return kotlinx.coroutines.CompletableDeferredKt.awaitCancellation(cont);
                }
                finally {
                    editorFactory.releaseEditor(editor);
                }
            }
        );
        return editor;
    }

    public static @Nonnull JComponent createDiffWithHeader(
        @Nonnull CoroutineScope cs,
        @Nonnull CollapsibleTimelineItemViewModel collapseVm,
        @Nonnull String filePath,
        @Nonnull Flow<ActionListener> fileNameClickListener,
        @Nonnull Function<CoroutineScope, JComponent> diffComponentFactory
    ) {
        InlineIconButton expandCollapseButton = new InlineIconButton(EmptyIcon.ICON_16);
        // Button state binding done from Kotlin side via coroutine collection
        BindVisibilityInKt.bindVisibilityIn(expandCollapseButton, cs, collapseVm.getCollapsible());

        ClippingRoundedPanel panel =
            new ClippingRoundedPanel(8, CodeReviewCommentUIUtil.COMMENT_BUBBLE_BORDER_COLOR, ListLayout.vertical(0));
        panel.setBackground(JBColor.lazy(() -> EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()));

        panel.add(createFileNameComponent(cs, filePath, expandCollapseButton, fileNameClickListener));
        BindChildInKt.bindChildIn(panel, cs, FlowKt.distinctUntilChanged(collapseVm.getCollapsed()), (scope, collapsed) -> {
            if ((Boolean) collapsed) {
                return null;
            }
            JComponent diffComp = diffComponentFactory.apply(scope);
            diffComp.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
            return diffComp;
        });

        return panel;
    }

    public static @Nonnull JComponent createDiffWithHeader(
        @Nonnull CoroutineScope cs,
        @Nonnull String filePath,
        @Nonnull Flow<ActionListener> fileNameClickListener,
        @Nonnull JComponent diffComponent
    ) {
        ClippingRoundedPanel panel =
            new ClippingRoundedPanel(8, CodeReviewCommentUIUtil.COMMENT_BUBBLE_BORDER_COLOR, ListLayout.vertical());
        panel.setBackground(JBColor.lazy(() -> EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()));

        panel.add(createFileNameComponent(cs, filePath, null, fileNameClickListener));
        diffComponent.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
        panel.add(diffComponent);

        return panel;
    }

    private static @Nonnull JComponent createFileNameComponent(
        @Nonnull CoroutineScope cs,
        @Nonnull String filePath,
        @Nullable JComponent expandCollapseButton,
        @Nonnull Flow<ActionListener> nameClickListener
    ) {
        String name = PathUtil.getFileName(filePath);
        String path = PathUtil.getParentPath(filePath);
        var fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(name);

        ActionLink nameLabel = new ActionLink(name);
        nameLabel.setIcon(fileType.getIcon());
        nameLabel.setAutoHideOnDisable(false);

        // Collection of nameClickListener done from Kotlin side
        // Simplified stub: the coroutine-based collection must be wired from Kotlin

        JPanel panel = new JPanel(new MigLayout(new LC().insets("0").gridGap("5", "0").fill().noGrid()));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(10));

        panel.add(nameLabel);

        if (!path.isBlank()) {
            JLabel pathLabel = new JLabel(path);
            pathLabel.setForeground(UIUtil.getContextHelpForeground());
            panel.add(pathLabel, new CC().minWidth("0"));
        }

        if (expandCollapseButton != null) {
            panel.add(expandCollapseButton, new CC().hideMode(3).gapLeft("10:push"));
        }

        return panel;
    }
}
