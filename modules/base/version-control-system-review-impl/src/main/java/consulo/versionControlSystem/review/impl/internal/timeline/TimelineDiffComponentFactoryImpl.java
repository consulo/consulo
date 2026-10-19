// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.review.impl.internal.timeline;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.LineNumberConverter;
import consulo.codeEditor.LineNumberConverterAdapter;
import consulo.diff.impl.internal.util.DiffDrawUtil;
import consulo.diff.internal.LineNumberConvertor;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.diff.util.TextDiffType;
import consulo.diff.util.TextDiffTypeFactory;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.timeline.CollapsibleTimelineItemViewModel;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.io.PathUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.PatchHunkUtil;
import consulo.versionControlSystem.change.patch.PatchLine;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import consulo.versionControlSystem.impl.internal.patch.apply.GenericPatchApplier;
import consulo.versionControlSystem.impl.internal.patch.tool.PatchChangeBuilder;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import consulo.versionControlSystem.review.timeline.TimelineDiffComponentFactory;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@Singleton
@ServiceImpl
public class TimelineDiffComponentFactoryImpl implements TimelineDiffComponentFactory {
    public static final TextDiffType ANCHOR_LINE =
        new TextDiffTypeFactory.TextDiffTypeImpl(CodeReviewColorUtil.REVIEW_TIMELINE_DIFF_ANCHOR_LINE, "Comment Anchor Line");

    private final Project myProject;
    private final EditorBoxBuilderFactory myEditorBoxBuilderFactory;
    private final EditorFactory myEditorFactory;

    @Inject
    public TimelineDiffComponentFactoryImpl(Project project, EditorBoxBuilderFactory editorBoxBuilderFactory, EditorFactory editorFactory) {
        myProject = project;
        myEditorBoxBuilderFactory = editorBoxBuilderFactory;
        myEditorFactory = editorFactory;
    }

    @RequiredUIAccess
    @Override
    public Component createDiffComponent(Disposable parentDisposable,
                                         PatchHunk patchHunk,
                                         DiffLineLocation anchor,
                                         @Nullable DiffLineLocation anchorStart) {
        PatchHunk truncatedHunk = truncateHunk(patchHunk, anchor, anchorStart);

        Integer anchorLineIndex = PatchHunkUtil.findHunkLineIndex(truncatedHunk, toPair(anchor));
        Integer anchorStartLineIndex = anchorStart != null && !anchorStart.equals(anchor)
            ? PatchHunkUtil.findHunkLineIndex(truncatedHunk, toPair(anchorStart))
            : null;
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

        return createDiffComponent(parentDisposable, truncatedHunk, anchorRange);
    }

    @RequiredUIAccess
    @Override
    public Component createDiffComponent(Disposable parentDisposable, PatchHunk patchHunk, @Nullable LineRange anchorLineRange) {
        boolean hasChanges = false;
        for (PatchLine line : patchHunk.getLines()) {
            if (line.getType() != PatchLine.Type.CONTEXT) {
                hasChanges = true;
                break;
            }
        }

        if (hasChanges) {
            List<AppliedTextPatch.AppliedSplitPatchHunk> appliedSplitHunks = new ArrayList<>();
            for (GenericPatchApplier.SplitHunk splitHunk : GenericPatchApplier.SplitHunk.read(patchHunk)) {
                appliedSplitHunks.add(new AppliedTextPatch.AppliedSplitPatchHunk(splitHunk, -1, -1, AppliedTextPatch.HunkStatus.NOT_APPLIED));
            }

            PatchChangeBuilder state = new PatchChangeBuilder();
            state.exec(appliedSplitHunks);

            String patchContent = StringUtil.trimEnd(state.getPatchContent().toString(), "\n");
            LineNumberConvertor lineConvertor = state.getLineConvertor();
            List<PatchChangeBuilder.Hunk> hunks = state.getHunks();

            return createDiffEditor(patchContent, editor -> {
                editor.getGutterComponentEx().setLineNumberConverter(new LineNumberConverterAdapter(lineConvertor.createConvertor1()),
                    new LineNumberConverterAdapter(lineConvertor.createConvertor2()));

                for (PatchChangeBuilder.Hunk hunk : hunks) {
                    DiffDrawUtil.createUnifiedChunkHighlighters(editor, hunk.patchDeletionRange(), hunk.patchInsertionRange(), null);
                }
                if (anchorLineRange != null) {
                    highlightAnchor(editor, anchorLineRange);
                }
            });
        }
        else {
            String patchContent = StringUtil.trimEnd(patchHunk.getText(), "\n");

            return createDiffEditor(patchContent, editor -> {
                editor.getGutterComponentEx().setLineNumberConverter(
                    (LineNumberConverter.Increasing) (e, line) -> line + patchHunk.getStartLineBefore(),
                    (LineNumberConverter.Increasing) (e, line) -> line + patchHunk.getStartLineAfter()
                );
                if (anchorLineRange != null) {
                    highlightAnchor(editor, anchorLineRange);
                }
            });
        }
    }

    @RequiredUIAccess
    @Override
    public Component createDiffWithHeader(Disposable parentDisposable,
                                          CollapsibleTimelineItemViewModel collapseVm,
                                          String filePath,
                                          ObservableValue<@Nullable Runnable> fileNameClickListener,
                                          Function<Disposable, Component> diffComponentFactory) {
        Button expandCollapseButton = Button.create(LocalizeValue.empty());
        expandCollapseButton.addStyle(ButtonStyle.INPLACE);
        expandCollapseButton.addClickListener(event -> collapseVm.setCollapsed(!collapseVm.getCollapsed().get()));
        ReviewUIUtil.bind(parentDisposable, collapseVm.getCollapsed(), collapsed -> expandCollapseButton.setIcon(collapsed
            ? PlatformIconGroup.generalExpandcomponent()
            : PlatformIconGroup.generalCollapsecomponent()));
        ReviewUIUtil.bind(parentDisposable, collapseVm.getCollapsible(), expandCollapseButton::setVisible);

        Card card = createDiffCard(parentDisposable, filePath, expandCollapseButton, fileNameClickListener);

        VerticalLayout body = VerticalLayout.create(Space.NONE);
        card.add(body);

        AtomicReference<@Nullable Disposable> diffDisposable = new AtomicReference<>();
        Disposer.register(parentDisposable, () -> disposeDiff(diffDisposable));
        ReviewUIUtil.bind(parentDisposable, collapseVm.getCollapsed(), collapsed -> {
            disposeDiff(diffDisposable);
            body.removeAll();
            if (!collapsed) {
                Disposable disposable = Disposable.newDisposable();
                diffDisposable.set(disposable);
                body.add(diffComponentFactory.apply(disposable));
            }
        });
        return card;
    }

    private static void disposeDiff(AtomicReference<@Nullable Disposable> diffDisposable) {
        Disposable disposable = diffDisposable.getAndSet(null);
        if (disposable != null) {
            Disposer.dispose(disposable);
        }
    }

    @RequiredUIAccess
    @Override
    public Component createDiffWithHeader(Disposable parentDisposable,
                                          String filePath,
                                          ObservableValue<@Nullable Runnable> fileNameClickListener,
                                          Component diffComponent) {
        Card card = createDiffCard(parentDisposable, filePath, null, fileNameClickListener);
        card.add(diffComponent);
        return card;
    }

    @RequiredUIAccess
    private static Card createDiffCard(Disposable parentDisposable,
                                       String filePath,
                                       @Nullable Component expandCollapseButton,
                                       ObservableValue<@Nullable Runnable> fileNameClickListener) {
        Card card = Card.create();
        card.addStyle(CardStyle.OUTLINED);
        card.addStyle(CardStyle.COMPACT);
        card.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
        card.setHeader(createFileNameComponent(parentDisposable, filePath, expandCollapseButton, fileNameClickListener));
        return card;
    }

    @RequiredUIAccess
    private static Component createFileNameComponent(Disposable parentDisposable,
                                                     String filePath,
                                                     @Nullable Component expandCollapseButton,
                                                     ObservableValue<@Nullable Runnable> nameClickListener) {
        String name = PathUtil.getFileName(filePath);
        String path = PathUtil.getParentPath(filePath);
        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(name);

        Hyperlink nameLabel = Hyperlink.create(LocalizeValue.of(name), event -> {
            Runnable listener = nameClickListener.get();
            if (listener != null) {
                listener.run();
            }
        });
        nameLabel.setIcon(fileType.getIcon());
        ReviewUIUtil.bind(parentDisposable, nameClickListener, listener -> nameLabel.setEnabled(listener != null));

        HorizontalLayout left = HorizontalLayout.create(Space.SMALL);
        left.add(nameLabel);
        if (!path.isBlank()) {
            Label pathLabel = Label.create(LocalizeValue.of(path));
            pathLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            left.add(pathLabel);
        }

        DockLayout header = DockLayout.create(Space.NONE);
        header.left(left);
        if (expandCollapseButton != null) {
            header.right(expandCollapseButton);
        }
        return header;
    }

    private static void highlightAnchor(EditorEx editor, LineRange lineRange) {
        DiffDrawUtil.createHighlighter(editor, lineRange.start(), lineRange.end(), ANCHOR_LINE, false);
    }

    private static Pair<Side, Integer> toPair(DiffLineLocation location) {
        return Pair.create(location.side(), location.line());
    }

    private static PatchHunk truncateHunk(PatchHunk hunk, DiffLineLocation anchor, @Nullable DiffLineLocation anchorStart) {
        if (hunk.getLines().size() <= DIFF_CONTEXT_SIZE + 1) {
            return hunk;
        }
        DiffLineLocation actualAnchorStart = anchorStart != null && !anchorStart.equals(anchor) ? anchorStart : anchor;
        return truncateHunkAfter(truncateHunkBefore(hunk, actualAnchorStart), anchor);
    }

    private static PatchHunk truncateHunkBefore(PatchHunk hunk, DiffLineLocation location) {
        List<PatchLine> lines = hunk.getLines();
        if (lines.size() <= DIFF_CONTEXT_SIZE + 1) {
            return hunk;
        }
        Integer lineIdx = PatchHunkUtil.findHunkLineIndex(hunk, toPair(location));
        if (lineIdx == null) {
            return hunk;
        }
        int startIdx = lineIdx - DIFF_CONTEXT_SIZE;
        return PatchHunkUtil.truncateHunkBefore(hunk, startIdx);
    }

    private static PatchHunk truncateHunkAfter(PatchHunk hunk, DiffLineLocation location) {
        List<PatchLine> lines = hunk.getLines();
        if (lines.size() <= DIFF_CONTEXT_SIZE + 1) {
            return hunk;
        }
        Integer lineIdx = PatchHunkUtil.findHunkLineIndex(hunk, toPair(location));
        if (lineIdx == null) {
            return hunk;
        }
        int endIdx = lineIdx + DIFF_CONTEXT_SIZE;
        return PatchHunkUtil.truncateHunkAfter(hunk, endIdx);
    }

    /**
     * A read-only editor of the text: no caret, no vertical scroll, sized to its lines.
     *
     * @param customization runs every time the editor is created, after the box is attached
     */
    @RequiredUIAccess
    public EditorBox createDiffEditor(CharSequence text, Consumer<EditorEx> customization) {
        Document document = myEditorFactory.createDocument(text);
        return myEditorBoxBuilderFactory.create(myProject)
            .document(document)
            .viewer()
            .multiline()
            .editorFont()
            .customize(editor -> {
                editor.setRendererMode(true);
                editor.setHorizontalScrollbarVisible(true);
                editor.setVerticalScrollbarVisible(false);
                editor.setCaretEnabled(false);
                editor.setEmbeddedIntoDialogWrapper(true);

                EditorSettings settings = editor.getSettings();
                settings.setLineNumbersShown(true);
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

                customization.accept(editor);
            })
            .build();
    }
}
