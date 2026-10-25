// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.diff.FrameDiffTool;
import consulo.diff.impl.internal.fragment.UnifiedDiffViewer;
import consulo.diff.impl.internal.viewer.DiffViewerBase;
import consulo.diff.impl.internal.viewer.DiffViewerListener;
import consulo.diff.impl.internal.viewer.side.OnesideTextDiffViewer;
import consulo.diff.impl.internal.viewer.side.TwosideTextDiffViewer;
import consulo.diff.util.LineCol;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.review.editor.CodeReviewCommentableEditorModel;
import consulo.review.editor.CodeReviewEditorModel;
import consulo.review.editor.CodeReviewEditorRenderer;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.review.editor.CodeReviewNavigableEditorViewModel;
import consulo.review.editor.ReviewInEditorUtil;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.diff.CodeReviewDiffViewerRenderer;
import consulo.versionControlSystem.review.diff.DiffEditorLineMapper;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import consulo.versionControlSystem.review.diff.DiffMapped;
import consulo.versionControlSystem.review.diff.EditorCodeReviewRenderer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

@Singleton
@ServiceImpl
public class CodeReviewDiffViewerRendererImpl implements CodeReviewDiffViewerRenderer {
    private final CodeReviewEditorRenderer myEditorRenderer;

    @Inject
    public CodeReviewDiffViewerRendererImpl(CodeReviewEditorRenderer editorRenderer) {
        myEditorRenderer = editorRenderer;
    }

    @RequiredUIAccess
    @Override
    public void renderCodeReview(Disposable parentDisposable, FrameDiffTool.DiffViewer viewer, EditorCodeReviewRenderer editorRenderer) {
        if (!(viewer instanceof DiffViewerBase viewerBase)) {
            return;
        }

        SequentialDisposable renderSession = new SequentialDisposable(parentDisposable);
        boolean[] ready = {false};
        Runnable update = () -> {
            boolean viewerReady = !viewerBase.hasPendingRediff();
            if (viewerReady == ready[0]) {
                return;
            }
            ready[0] = viewerReady;
            renderSession.restart(scope -> {
                if (viewerReady) {
                    render(scope, viewerBase, editorRenderer);
                }
            });
        };
        DiffViewerListener listener = new DiffViewerListener() {
            @Override
            protected void onAfterRediff() {
                update.run();
            }
        };
        viewerBase.addListener(listener);
        Disposer.register(parentDisposable, () -> viewerBase.removeListener(listener));
        update.run();
    }

    @RequiredUIAccess
    @Override
    public <VM extends DiffMapped> void controlInlaysIn(Disposable parentDisposable,
                                                        FrameDiffTool.DiffViewer viewer,
                                                        ObservableValue<? extends Collection<VM>> vms,
                                                        Function<VM, Object> vmKeyExtractor,
                                                        BiFunction<Disposable, VM, @Nullable Component> rendererFactory) {
        renderCodeReview(parentDisposable, viewer, (scope, editor, side, mapper) -> {
            ObservableValue<List<DiffMappedInlayModel<VM>>> inlays = new ObservableValue<>() {
                @Override
                public List<DiffMappedInlayModel<VM>> get() {
                    return wrap(vms.get());
                }

                @Override
                public Runnable addListener(Consumer<? super List<DiffMappedInlayModel<VM>>> listener) {
                    return vms.addListener(value -> listener.accept(wrap(value)));
                }

                private List<DiffMappedInlayModel<VM>> wrap(Collection<VM> value) {
                    List<DiffMappedInlayModel<VM>> result = new ArrayList<>(value.size());
                    for (VM vm : value) {
                        result.add(new DiffMappedInlayModel<>(vm, vmKeyExtractor.apply(vm), mapper));
                    }
                    return result;
                }
            };
            myEditorRenderer.renderInlays(scope, editor, inlays, null, (inlayScope, inlay) -> rendererFactory.apply(inlayScope, inlay.getVm()));
        });
    }

    @RequiredUIAccess
    private static void render(Disposable scope, DiffViewerBase viewer, EditorCodeReviewRenderer editorRenderer) {
        if (viewer instanceof OnesideTextDiffViewer oneside) {
            Side side = oneside.getSide();
            Editor editor = oneside.getEditor();
            editorRenderer.render(scope, editor, side, mapper(
                location -> location.side() == side ? location.line() : null,
                line -> ReviewInEditorUtil.isLastBlankLine(editor.getDocument(), line) ? null : new DiffLineLocation(side, line),
                line -> side == Side.LEFT ? new int[]{line, -1} : new int[]{-1, line}
            ));
        }
        else if (viewer instanceof UnifiedDiffViewer unified) {
            editorRenderer.render(scope, unified.getEditor(), null, mapper(
                location -> {
                    int line = unified.transferLineToOnesideStrict(location.side(), location.line());
                    return line >= 0 ? line : null;
                },
                line -> {
                    Side side = unified.transferLineFromOneside(line).second;
                    int sideLineIdx = unified.transferLineFromOnesideStrict(side, line);
                    if (sideLineIdx < 0) {
                        return null;
                    }
                    // Check line against the document of original side
                    Document document = unified.getDocument(side);
                    if (ReviewInEditorUtil.isLastBlankLine(document, sideLineIdx)) {
                        return null;
                    }
                    return new DiffLineLocation(side, sideLineIdx);
                },
                line -> unified.transferLineFromOneside(line).first
            ));
        }
        else if (viewer instanceof TwosideTextDiffViewer twoside) {
            Editor editor1 = twoside.getEditor1();
            editorRenderer.render(scope, editor1, Side.LEFT, mapper(
                location -> location.side() == Side.LEFT ? location.line() : null,
                line -> ReviewInEditorUtil.isLastBlankLine(editor1.getDocument(), line) ? null : new DiffLineLocation(Side.LEFT, line),
                line -> new int[]{line, twoside.transferPosition(Side.LEFT, new LineCol(line, 0)).line()}
            ));
            Editor editor2 = twoside.getEditor2();
            editorRenderer.render(scope, editor2, Side.RIGHT, mapper(
                location -> location.side() == Side.RIGHT ? location.line() : null,
                line -> ReviewInEditorUtil.isLastBlankLine(editor2.getDocument(), line) ? null : new DiffLineLocation(Side.RIGHT, line),
                line -> new int[]{twoside.transferPosition(Side.RIGHT, new LineCol(line, 0)).line(), line}
            ));
        }
    }

    private static DiffEditorLineMapper mapper(Function<DiffLineLocation, @Nullable Integer> locationToLine,
                                               IntFunction<@Nullable DiffLineLocation> lineToLocation,
                                               IntFunction<int[]> lineToUnified) {
        return new DiffEditorLineMapper() {
            @Override
            public @Nullable Integer locationToLine(DiffLineLocation location) {
                return locationToLine.apply(location);
            }

            @Override
            public @Nullable DiffLineLocation lineToLocation(int line) {
                return lineToLocation.apply(line);
            }

            @Override
            public int[] lineToUnified(int line) {
                return lineToUnified.apply(line);
            }
        };
    }

    @RequiredUIAccess
    @Override
    public <I extends CodeReviewInlayModel> void showCodeReview(Disposable parentDisposable, Editor editor, CodeReviewEditorModel<I> model) {
        myEditorRenderer.setupIn(parentDisposable, editor, model);

        editor.putUserData(CodeReviewCommentableEditorModel.KEY, model);
        if (model instanceof CodeReviewNavigableEditorViewModel navigable) {
            editor.putUserData(CodeReviewNavigableEditorViewModel.KEY, navigable);
        }
        Disposer.register(parentDisposable, () -> {
            editor.putUserData(CodeReviewCommentableEditorModel.KEY, null);
            if (model instanceof CodeReviewNavigableEditorViewModel) {
                editor.putUserData(CodeReviewNavigableEditorViewModel.KEY, null);
            }
        });
    }
}
