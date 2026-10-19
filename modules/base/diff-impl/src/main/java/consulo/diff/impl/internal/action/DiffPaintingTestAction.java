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
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.DumbProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeFactory;
import consulo.codeEditor.internal.bridge.EditorBridgeFoldConnector;
import consulo.codeEditor.internal.bridge.EditorScrollMap;
import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.diff.comparison.ComparisonManager;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.fragment.LineFragment;
import consulo.diff.impl.internal.util.DiffDrawUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.util.TextDiffType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ActionImpl(id = "DiffPaintingTestAction", parents = @ActionParentRef(@ActionRef(id = "Internal.UI")))
public class DiffPaintingTestAction extends AnAction implements DumbAware {
    private static final String LEFT = """
        package demo;

        class Sample {
            int first = 1;
            int second = 2;
            int third = 3;
            int fourth = 4;
            int fifth = 5;
            int sixth = 6;
            int seventh = 7;
            int eighth = 8;
            String name = "left side";
            void removed() {
            }
            void common() {
                call(alpha, beta);
            }
        }
        """;

    private static final String RIGHT = """
        package demo;

        class Sample {
            int first = 1;
            int second = 2;
            int third = 3;
            int fourth = 4;
            int fifth = 5;
            int sixth = 6;
            int seventh = 7;
            int eighth = 8;
            String name = "right side";
            void common() {
                call(alpha, gamma, beta);
            }
            void added() {
            }
        }
        """;

    private static final String BASE = """
        package demo;

        class Sample {
            int first = 1;
            int second = 2;
            int third = 3;
            int fourth = 4;
            int fifth = 50;
            int sixth = 6;
            int seventh = 7;
            int eighth = 8;
            String name = "base side";
            void common() {
                call(alpha, beta);
            }
        }
        """;

    private static final int TAIL_LINES = 500;

    private static final int FOLD_START_LINE = 4;
    private static final int FOLD_END_LINE = 9;

    private final DialogService myDialogService;
    private final EditorFactory myEditorFactory;
    private final EditorBridgeFactory myEditorBridgeFactory;

    @Inject
    public DiffPaintingTestAction(DialogService dialogService, EditorFactory editorFactory, EditorBridgeFactory editorBridgeFactory) {
        super(LocalizeValue.localizeTODO("Diff Painting Test"));
        myDialogService = dialogService;
        myEditorFactory = editorFactory;
        myEditorBridgeFactory = editorBridgeFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        myDialogService.build(new DialogDescriptor(LocalizeValue.localizeTODO("Diff Painting Test")) {
            @RequiredUIAccess
            @Override
            public Component createCenterComponent(Disposable uiDisposable) {
                String leftText = leftText();
                String rightText = rightText();
                Editor left = createEditor(project, leftText, uiDisposable);
                Editor right = createEditor(project, rightText, uiDisposable);
                ((EditorEx) left).setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);

                List<LineFragment> fragments = ComparisonManager.getInstance()
                    .compareLinesInner(leftText, rightText, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);

                List<int[]> anchors = new ArrayList<>();
                anchors.add(new int[]{0, 0});
                List<EditorBridgeConnector> connectors = new ArrayList<>();

                for (LineFragment fragment : fragments) {
                    TextDiffType type = DiffImplUtil.getLineDiffType(fragment);
                    anchors.add(new int[]{fragment.getStartLine1(), fragment.getStartLine2()});
                    anchors.add(new int[]{fragment.getEndLine1(), fragment.getEndLine2()});
                    connectors.add(new EditorBridgeConnector(
                        fragment.getStartLine1(),
                        fragment.getEndLine1(),
                        fragment.getStartLine2(),
                        fragment.getEndLine2(),
                        type.getColor(left),
                        false
                    ));
                    DiffDrawUtil.createHighlighter(left, fragment.getStartLine1(), fragment.getEndLine1(), type, false);
                    DiffDrawUtil.createHighlighter(right, fragment.getStartLine2(), fragment.getEndLine2(), type, false);

                    List<DiffFragment> innerFragments = fragment.getInnerFragments();
                    if (innerFragments == null) {
                        continue;
                    }

                    int leftStart = left.getDocument().getLineStartOffset(fragment.getStartLine1());
                    int rightStart = right.getDocument().getLineStartOffset(fragment.getStartLine2());
                    for (DiffFragment innerFragment : innerFragments) {
                        TextDiffType innerType = DiffImplUtil.getDiffType(innerFragment);
                        DiffDrawUtil.createInlineHighlighter(left,
                            leftStart + innerFragment.getStartOffset1(), leftStart + innerFragment.getEndOffset1(), innerType);
                        DiffDrawUtil.createInlineHighlighter(right,
                            rightStart + innerFragment.getStartOffset2(), rightStart + innerFragment.getEndOffset2(), innerType);
                    }
                }

                for (Editor editor : List.of(left, right)) {
                    DiffDrawUtil.createBorderLineMarker(editor, 1, SeparatorPlacement.BOTTOM);
                    foldUnchanged(editor);
                }

                anchors.add(new int[]{left.getDocument().getLineCount(), right.getDocument().getLineCount()});

                EditorBridge bridge = myEditorBridgeFactory.create(List.of(left, right), uiDisposable);
                bridge.setScrollMap(new EditorScrollMap(anchors));
                bridge.setConnectors(0, connectors, List.of(new EditorBridgeFoldConnector(FOLD_START_LINE, FOLD_START_LINE)));
                return bridge.getComponent();
            }

            @Override
            public boolean hasDefaultContentBorder() {
                return false;
            }

            @Override
            public @Nullable WidthAndHeight getInitialSize() {
                return WidthAndHeight.ofFont(110, 30);
            }
        }).showAsync();
    }

    static String leftText() {
        return LEFT + tail(false);
    }

    static String rightText() {
        return RIGHT + tail(true);
    }

    static String baseText() {
        return BASE + tail(false);
    }

    private static String tail(boolean right) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < TAIL_LINES; i++) {
            if (i % 50 == 25) {
                builder.append("    String long").append(i).append(" = \"")
                    .append("a very long line which does not fit into the editor, ".repeat(6))
                    .append(right ? "and it ends differently on the right side" : "and it ends on the left side")
                    .append("\";\n");
            }
            else if (i % 100 == 60) {
                if (!right) {
                    builder.append("    // removed line ").append(i).append('\n');
                }
            }
            else if (i % 100 == 80 && right) {
                builder.append("    // added line ").append(i).append('\n');
                builder.append("    // tail line ").append(i).append('\n');
            }
            else {
                builder.append("    // tail line ").append(i).append('\n');
            }
        }
        return builder.toString();
    }

    @RequiredUIAccess
    private Editor createEditor(@Nullable Project project, String text, Disposable uiDisposable) {
        Document document = myEditorFactory.createDocument(text);
        Editor editor = myEditorFactory.createViewer(document, project, EditorKind.DIFF);
        Disposer.register(uiDisposable, () -> myEditorFactory.releaseEditor(editor));
        return editor;
    }

    @RequiredUIAccess
    private static void foldUnchanged(Editor editor) {
        Document document = editor.getDocument();
        int start = document.getLineStartOffset(FOLD_START_LINE);
        int end = document.getLineEndOffset(FOLD_END_LINE);

        editor.getFoldingModel().runBatchFoldingOperation(() -> {
            FoldRegion region = editor.getFoldingModel().addFoldRegion(start, end, "");
            if (region != null) {
                region.setExpanded(false);
            }
        });

        DiffDrawUtil.createLineSeparatorHighlighter(editor, start, end, () -> true);
    }
}
